package unipi.msss.foodback.home.ui

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.data.SessionManager
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.home.data.HomeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager
import unipi.msss.foodback.R
import unipi.msss.foodback.home.data.EEGNetClassifier
import unipi.msss.foodback.services.Sender
import unipi.msss.foodback.services.WearableData
import unipi.msss.foodback.services.WearableMessageListener
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeUseCase: HomeUseCase,
    viewModelEvents: ViewModelEvents<HomeNavigationEvents>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
) : EventStateViewModel<HomeState, HomeEvent>(),
    ViewModelEvents<HomeNavigationEvents> by viewModelEvents {

    override val _state: MutableStateFlow<HomeState> = MutableStateFlow(
        HomeState(name = sessionManager.userName)
    )
    private val wearableMessageListener = WearableMessageListener()
    private var healthCheckJob: Job? = null
    private var isReceivingData: Boolean = false
    private var isWatchAlive: Boolean = false
    private var serverManager: ServerManager? = null
    private var actualNumberOfMeasurement: Int = 0
    private var job: Job? = null
    private val buffer = mutableListOf<SensorData>()
    private val heartRateBuffer = mutableListOf<WearableData>()
    private val edaBuffer = mutableListOf<WearableData>()
    private val inferenceDuration: Int = 2
    private val targetFrequency: Int = 125
    private var samplingFrequencyEEG : Int = 500
    private val classifier: EEGNetClassifier


    init {
        startHealthCheck()
        startSession()
        classifier = EEGNetClassifier(context)
        Wearable.getMessageClient(context).addListener(wearableMessageListener)
    }
    override fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.ShowLogoutDialog -> updateState(_state.value.copy(showLogoutDialog = true))
            is HomeEvent.DismissLogoutDialog -> updateState(_state.value.copy(showLogoutDialog = false))
            is HomeEvent.ConfirmLogout -> {
                performLogout()
            }

            is HomeEvent.StartEvaluation -> {
                runInferenceProtocol()
            }
        }
    }

    companion object {
        private const val TIME_TO_GET_READY = 5_000L // ms
        private const val TIME_TO_INFERENCE = 2_000L // ms
    }

    /**
     * This function is used to start the health check process (if devices are available and ready).
     * It checks if the wearable device is connected and starts the wearable health check.
     * It checks if the Mindrove EEG is connected and ready to send data.
     */
    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch(ioDispatcher) {
            while (true) {
                delay(2000L)
                if(Wearable.getNodeClient(context).connectedNodes.await().isNotEmpty()) {
                    startWearableHealthCheck()
                    collectWearableHealthCheck()
                }
                if(isWatchAlive) {
                    _state.value = _state.value.copy(isWatchConnected = true)
                    isWatchAlive = false
                } else {
                    _state.value = _state.value.copy(isWatchConnected = false)
                }
                if(isReceivingData) {
                    _state.value = _state.value.copy(isEEGConnected = true)
                    isReceivingData = false
                }
                else{
                    _state.value = _state.value.copy(isEEGConnected = false)
                }

            }
        }
    }

    /**
     * This function is used to start the session with the Mindrove EEG server from SDK.
     * It creates a new instance of the ServerManager class and starts it.
     * It appends data in the buffer if there is a recording session.
     * In inference mode, the actualNumberOfMeasurement has to be 1000 samples.
     * In collection mode, the actualNumberOfMeasurement has to be 5000 samples.
     */
    private fun startSession() {
        serverManager = ServerManager { sensorData: SensorData ->
            isReceivingData = sensorData.numberOfMeasurement.toInt() != 0
            if (actualNumberOfMeasurement > 0) {
                actualNumberOfMeasurement--
                buffer += sensorData
            }
        }
        serverManager?.start()
        // Logging serverManager ip address
        Log.d("ServerManager", "ServerManager IP: ${serverManager?.ipAddress}")
    }

    /**
     * This function is used to collect wearable data from the wearable device.
     * It collects heart rate and EDA data from the wearable device.
     */
    private fun collectWearableData() {
        viewModelScope.launch {
            WearableMessageListener.heartRateFlow.collect { heartRates ->
                if (heartRates.isNotEmpty()) {
                    Log.d("HeartRateViewModel", "Received heart rate data: $heartRates")
                    heartRateBuffer.clear()
                    heartRateBuffer.addAll(heartRates)
                }
            }
        }

        viewModelScope.launch {
            WearableMessageListener.edaFlow.collect { edaValues ->
                if (edaValues.isNotEmpty()) {
                    Log.d("HeartRateViewModel", "Received EDA data: $edaValues")
                    edaBuffer.clear()
                    edaBuffer.addAll(edaValues)
                }
            }
        }
    }

    /**
     * This function implements the flow of the inference protocol.
     * It follows these steps:
     * 1. Plays a beep sound to indicate the start of the preparation phase.
     * 2. Waits for 5 seconds to allow the user to get ready.
     * 3. Plays a beep sound to indicate the start of the data collection phase.
     * 4. Starts sensing asynchronously for the collection of wearable data (heart rate and EDA).
     * 5. It performs the inference phase.
     */
    private fun runInferenceProtocol(){

        job = viewModelScope.launch(ioDispatcher) {
            _state.value = _state.value.copy(stage = HomeStage.InPreparation)
            MediaPlayer.create(context, R.raw.beep).apply {
                setOnCompletionListener { release() }
                start()
            }
            delay(TIME_TO_GET_READY)

            _state.value = _state.value.copy(stage = HomeStage.InProgress)
            buffer.clear()
            actualNumberOfMeasurement = (TIME_TO_INFERENCE/1000).toInt() * samplingFrequencyEEG
            startWearableSampling()
            collectWearableData()
            MediaPlayer.create(context, R.raw.beep).apply {
                setOnCompletionListener { release() }
                start()
            }
            delay(TIME_TO_INFERENCE)

            delay(1000L)
            inference()
            _state.value = _state.value.copy(stage = HomeStage.Finished)

            MediaPlayer.create(context, R.raw.beep).apply {
                setOnCompletionListener { release() }
                start()
            }
        }
    }

    /**
     * This function performs the inference on the collected data.
     * It checks if the buffers are empty and if so, it sends an error event.
     * It prepares the data for the classifier and performs the inference.
     */
    private fun inference() {
        if (buffer.isEmpty() || heartRateBuffer.isEmpty() || edaBuffer.isEmpty()) {
            viewModelScope.launch {
                sendEvent(HomeNavigationEvents.Error("No data available for inference."))
                _state.value.copy(stage = HomeStage.Idle)
                return@launch
            }
        } else {
            Log.d("EEG-Inference", "B4 EEG data: ${buffer.size} samples")
            Log.d("EEG-Inference", "B4 HR data: ${heartRateBuffer.size} samples")
            Log.d("EEG-Inference", "B4 EDA data: ${edaBuffer.size} samples")
            Log.d("EEG-Samples", "EEG samples: ${buffer.joinToString(", ") { it.toString() }}")
            val upsampledHr = upsampleData(heartRateBuffer, targetFrequency, inferenceDuration)
            val upsampledEda = upsampleData(edaBuffer, targetFrequency, inferenceDuration)

            val eegChannelsData = prepareChannelsData(buffer)
            val hrData = Array(1) { toFlattened(upsampledHr) }
            val edaData = Array(1) { toFlattened(upsampledEda) }
            val classifier = EEGNetClassifier(context)

            val scores = classifier.classify(eegChannelsData, hrData, edaData)

            val predIdx = scores.indices.maxByOrNull { scores[it] } ?: -1
            Log.i("EEGNet", "Predicted = $predIdx (score=${scores[predIdx]})")
            Log.i("EEGNet", "Raw model output = ${scores.joinToString(", ")}")
            _state.value = _state.value.copy(predictedRating = predIdx)
            buffer.clear()
        }
    }

    /**
     * Converts a list of WearableData to a flattened FloatArray.
     *
     * @param data List of WearableData to be converted.
     * @return Flattened FloatArray containing the values of the WearableData.
     */
    private fun toFlattened(data: List<WearableData>): FloatArray =
        data.flatMap { listOf(it.value) }.toFloatArray()

    /**
     * Prepares the EEG data for the classifier.
     * It converts the list of SensorData to a matrix of shape (6, n),
     * where n is the number of samples.
     *
     * @param data List of SensorData to be converted.
     * @return Matrix of shape (6, n) containing the EEG data.
     */
    private fun prepareChannelsData(data: List<SensorData>): Array<FloatArray> {
        val n = data.size
        require(n > 0) { "EEG buffer is empty!" }

        val matrix = Array(6) { FloatArray(n) }

        for (i in 0 until n) {
            val sample = data[i]
            matrix[0][i] = sample.channel1.toFloat()
            matrix[1][i] = sample.channel2.toFloat()
            matrix[2][i] = sample.channel3.toFloat()
            matrix[3][i] = sample.channel4.toFloat()
            matrix[4][i] = sample.channel5.toFloat()
            matrix[5][i] = sample.channel6.toFloat()
        }
        return matrix
    }

    /**
     * Linearly interpolates wearable data to a uniform sampling rate.
     *
     * @param data List of WearableData with timestamps in milliseconds.
     * @param targetFreq Target sampling frequency in Hz (our case, 125).
     * @param duration Duration in seconds over which to interpolate (our case, 2).
     * @return Upsampled list with exactly (targetFreq × duration) points.
     */
    private fun upsampleData(
        data: List<WearableData>,
        targetFreq: Int,
        duration: Int
    ): List<WearableData> {

        if (data.isEmpty()) return emptyList()  // Just a safety check, normally
        // we should get at least 1 data points
        val paddedData = if (data.size == 1) {
            listOf(data.first(), data.first())
        } else {
            data
        }

        val sortedData = paddedData.sortedBy { it.timestamp }
        val start = sortedData.first().timestamp
        val end = start + duration * 1000
        val numSamples = targetFreq * duration

        // Generate uniform target timestamps
        val step = (end - start).toFloat() / (numSamples - 1)
        val targetTimestamps = List(numSamples) { i -> start + (i * step).roundToInt() }

        val inputTimestamps = sortedData.map { it.timestamp.toFloat() }
        val inputValues = sortedData.map { it.value }

        val interpolated = mutableListOf<WearableData>()
        var j = 0

        for (t in targetTimestamps) {
            while (j < inputTimestamps.lastIndex && inputTimestamps[j + 1] < t) {
                j++
            }
            val t1 = inputTimestamps.getOrElse(j) { t.toFloat() }
            val t2 = inputTimestamps.getOrElse(j + 1) { t.toFloat() }
            val v1 = inputValues.getOrElse(j) { 0f }
            val v2 = inputValues.getOrElse(j + 1) { 0f }

            val value = if (t2 != t1) {
                v1 + (v2 - v1) * ((t - t1) / (t2 - t1))
            } else {
                v1
            }

            interpolated += WearableData(t.toLong(), value)
        }
        return interpolated
    }

    /**
     * This function sends a message to the wearable device to start the health check.
     * It handles any exceptions that may occur during the process and sends an error event if needed.
     */
    private fun startWearableHealthCheck(){
        try {
            Sender.sendHealthCheck(context)

        } catch (e: Exception) {
            viewModelScope.launch {
                sendEvent(HomeNavigationEvents.Error("Error starting wearable sampling: ${e.message}"))
                _state.value.copy(stage = HomeStage.Idle)
                return@launch
            }
        }
    }

    /**
     * This function sends a message to the wearable device to start the sampling process.
     * It handles any exceptions that may occur during the process and sends an error event if needed.
     */
    private fun startWearableSampling() {
        try {
            Sender.sendSamplingMessage(context, true)

        } catch (e: Exception) {
            viewModelScope.launch {
                sendEvent(HomeNavigationEvents.Error("Error starting wearable sampling: ${e.message}"))
                _state.value.copy(stage = HomeStage.Idle)
                return@launch
            }
        }
    }

    /**
     * This function collects the health check data from the wearable device.
     * It updates the state of the view model based on the health check data received.
     */
    private fun collectWearableHealthCheck(){
        viewModelScope.launch {
            WearableMessageListener.healthCheckFlow.collect { healthCheck ->
                isWatchAlive = true
            }
        }
    }

    /**
     * This function performs the logout operation.
     * It calls the logout method from the home use case and updates the state of the view model.
     */
    private fun performLogout() = viewModelScope.launch {
        homeUseCase.logout()
        updateState(_state.value.copy(showLogoutDialog = false))
        sendEvent(HomeNavigationEvents.LoggedOut)
    }

    override fun onCleared() {
        super.onCleared()
        serverManager?.stop()
        // Logging serverManager stop
        Log.d("ServerManager", "ServerManager stop called")
        job?.cancel()
        classifier.close()
        healthCheckJob?.cancel()
    }
}
