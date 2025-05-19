package unipi.msss.foodback.home.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager
import unipi.msss.foodback.R
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.home.data.TastingUseCase
import unipi.msss.foodback.home.ui.TastingNavigationEvents.*
import unipi.msss.foodback.services.WearableMessageListener
import unipi.msss.foodback.services.Sender
import unipi.msss.foodback.services.WearableData
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class TastingViewModel @Inject constructor(
    private val tastingUseCase: TastingUseCase,
    viewModelEvents: ViewModelEvents<TastingNavigationEvents>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @ApplicationContext private val context: Context
) : EventStateViewModel<TastingState, TastingEvent>(),
    ViewModelEvents<TastingNavigationEvents> by viewModelEvents {

    override val _state: MutableStateFlow<TastingState> = MutableStateFlow(TastingState())

    private var healthCheckJob: Job? = null
    private val wearableMessageListener = WearableMessageListener()
    private val buffer = mutableListOf<SensorData>()
    private var isReceivingData: Boolean = false
    private var isWatchAlive: Boolean = false
    private var samplingFrequencyEEG : Int = 500
    private var actualNumberOfMeasurement: Int = 0
    private var job: Job? = null
    private var serverManager: ServerManager? = null
    private val heartRateBuffer = mutableListOf<WearableData>()
    private val edaBuffer = mutableListOf<WearableData>()

    init {
        startHealthCheck()
        startSession()
        Wearable.getMessageClient(context).addListener(wearableMessageListener)
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
                try {
                    val connectedNodes = Wearable.getNodeClient(context).connectedNodes.await()
                    if (connectedNodes.isNotEmpty()) {
                        startWearableHealthCheck()
                        collectWearableHealthCheck()
                    } else {
                        Log.e("WearableAPI", "No wearable devices connected.")
                    }
                } catch (e: Exception) {
                    Log.e("WearableAPI", "Error accessing Wearable API: ${e.message}", e)
                    sendEvent(Error("${e.message}"))
                    break
                }
                if (isWatchAlive) {
                    _state.value = _state.value.copy(isWatchConnected = true)
                    isWatchAlive = false
                } else {
                    _state.value = _state.value.copy(isWatchConnected = false)
                }
                if (isReceivingData) {
                    _state.value = _state.value.copy(isEEGConnected = true)
                    isReceivingData = false
                } else {
                    _state.value = _state.value.copy(isEEGConnected = false)
                }
            }
        }
    }

    companion object {
        private const val TIME_TO_GET_READY = 5_000L // ms
        private const val TIME_TO_TASTE = 10_000L // ms
    }

    override fun onEvent(event: TastingEvent) {
        when (event) {
            is TastingEvent.SubjectChanged -> {
                _state.value = _state.value.copy(subject = event.value)
                _state.value = _state.value.copy(stage = TastingStage.Idle)

            }
            is TastingEvent.StartProtocol -> {
                if (!isNetworkAvailable(context) || !state.value.isEEGConnected) {
                    viewModelScope.launch {
                        sendEvent(Error("Turn on the wifi and connect to your Mindrove device to start the protocol."))
                    }
                    return
                }
                buffer.clear()
                runProtocol()
            }

            is TastingEvent.RatingChanged -> {
                _state.value = _state.value.copy(rating = event.value)
            }

            is TastingEvent.SubmitRating -> {
                val rating = _state.value.rating.toIntOrNull() ?: return
                val subject = _state.value.subject
                submitRating(rating, subject)
            }

            is TastingEvent.DeleteEEGCsv -> {
                deleteCsvFile(context, "eeg_tasting_data.csv")
            }

            is TastingEvent.ShareEEGCsv -> {
                shareCsvFile(context, "eeg_tasting_data.csv")
            }

            is TastingEvent.DeleteEDACsv -> {
                deleteCsvFile(context, "eda_data.csv")
            }

            is TastingEvent.DeleteHRCsv -> {
                deleteCsvFile(context, "heart_rate_data.csv")
            }

            is TastingEvent.ShareEDACsv -> {
                shareCsvFile(context, "eda_data.csv")
            }

            is TastingEvent.ShareHRCsv -> {
                shareCsvFile(context, "heart_rate_data.csv")
            }

            is TastingEvent.ShowLogoutDialog -> {
                _state.value = _state.value.copy(showLogoutDialog = true)
            }

            is TastingEvent.DismissLogoutDialog -> {
                _state.value = _state.value.copy(showLogoutDialog = false)
            }

            is TastingEvent.ConfirmLogout -> {
                performLogout()
            }

            is TastingEvent.PreviewCheckboxClicked -> {
                _state.value = _state.value.copy(previewMode = !_state.value.previewMode)
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
                _state.value = _state.value.copy(sensorData = buffer.toList())
            }
        }
        serverManager?.start()
    }

    /**
     * This function is used to run the protocol for the tasting session.
     * It follows the steps of the tasting protocol:
     * 1. Start the tasting session
     * 2. Wait for the user to bring the sample to their mouth
     * 3. Start the recording
     * 4. Wait for the user to finish tasting
     * 5. Ask for the rating
     */
    private fun runProtocol() {
        job = viewModelScope.launch(ioDispatcher) {
            // First beep
            _state.value = _state.value.copy(stage = TastingStage.BringingToMouth)
            MediaPlayer.create(context, R.raw.beep).apply {
                setOnCompletionListener { release() }
                start()
            }
            delay(TIME_TO_GET_READY)

            // Second beep
            _state.value = _state.value.copy(stage = TastingStage.Recording)
            buffer.clear()
            actualNumberOfMeasurement = (TIME_TO_TASTE/1000).toInt() * samplingFrequencyEEG
            startWearableSampling()
            collectWearableData()
            MediaPlayer.create(context, R.raw.beep).apply {
                setOnCompletionListener { release() }
                start()
            }
            delay(TIME_TO_TASTE)

            // Third beep
            _state.value = _state.value.copy(stage = TastingStage.Finished)
            MediaPlayer.create(context, R.raw.beep).apply {
                setOnCompletionListener { release() }
                start()
            }
            _state.value = _state.value.copy(stage = TastingStage.AskingRating)
        }
    }

    /**
     * This function is used to submit the rating for the tasting session.
     * It saves the sensor data to a CSV file if the preview mode is not enabled.
     * It saves the wearable data to a CSV file if the preview mode is not enabled.
     * It clears the buffer and updates the state of the view model.
     */
    private fun submitRating(rating: Int, subject: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                if (buffer.isEmpty()) {
                    sendEvent(Error("No sensor data to save. Check the Mindrove connection."))
                    _state.value = _state.value.copy(stage = TastingStage.Idle)
                    _state.value = _state.value.copy(rating = "")
                    return@launch
                }
                if(!_state.value.previewMode) {
                    val file = File(context.getExternalFilesDir(null), "eeg_tasting_data.csv")
                    val (sampleNumber, experimentNumber) = getExperimentMetaData(file)

                    val rows = buffer.mapIndexed { index, sd ->
                        listOf(
                            (sampleNumber + index).toString(),
                            sd.channel1.toString(),
                            sd.channel2.toString(),
                            sd.channel3.toString(),
                            sd.channel4.toString(),
                            sd.channel5.toString(),
                            sd.channel6.toString(),
                            experimentNumber.toString(),
                            subject,
                            rating.toString()
                        ).joinToString(",")
                    }

                    if (!file.exists()) {
                        file.appendText("sample,ch1,ch2,ch3,ch4,ch5,ch6,experiment,subject,rating")
                    }
                    file.appendText(rows.joinToString("\n", "\n"))

                    if (heartRateBuffer.isNotEmpty()) {
                        writeWearableDataToCsv(
                            context,
                            "heart_rate_data.csv",
                            experimentNumber,
                            subject,
                            rating.toString(),
                            heartRateBuffer
                        )
                    }
                    if (edaBuffer.isNotEmpty()) {
                        writeWearableDataToCsv(
                            context,
                            "eda_data.csv",
                            experimentNumber,
                            subject,
                            rating.toString(),
                            edaBuffer
                        )
                    }
                }
                _state.value = _state.value.copy(stage = TastingStage.Done)
                _state.value = _state.value.copy(sensorData = emptyList())
                _state.value = _state.value.copy(rating = "")
                _state.value = _state.value.copy(subject = "")
                buffer.clear()
                sendEvent(Finished)
            } catch (e: Exception) {
                sendEvent(Error("Failed to save data: ${e.message}"))
            }
        }
    }

    /**
     * Utility function to get the experiment metadata from the CSV file.
     * It reads the last line of the file and extracts the sample number and experiment number.
     * If the file does not exist or is empty, it returns (0, 1).
     */
    private fun getExperimentMetaData(file: File): Pair<Int, Int> {
        if (!file.exists() || file.readText().isBlank()) {
            return (0 to 1)
        }

        val lastLine = file.readLines().lastOrNull()

        val sampleNumber = lastLine?.split(",")?.getOrNull(0)?.toIntOrNull()?.plus(1) ?: 0
        val experimentNumber = lastLine?.split(",")?.getOrNull(7)?.toIntOrNull()?.plus(1) ?: 1

        return (sampleNumber to experimentNumber)
    }

    /**
     * This function deletes the CSV file with the given name.
     * It checks if the file exists and deletes it.
     * Used for managing the CSV files created during the tasting session.
     */
    private fun deleteCsvFile(context: Context, fileName: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val file = File(context.getExternalFilesDir(null), fileName)

                if (file.exists()) {
                    if (file.delete()) {
                        sendEvent(Error("$fileName deleted successfully."))
                    } else {
                        sendEvent(Error("Failed to delete $fileName."))
                    }
                } else {
                    sendEvent(Error("$fileName does not exist."))
                }
            } catch (e: Exception) {
                sendEvent(Error("Failed to delete $fileName: ${e.message}"))
            }
        }
    }

    /**
     * This function shares the CSV file with the given name.
     * It uses the FileProvider to get the URI of the file and sends a ShareCsvFile event.
     * It checks if the file exists before sharing it.
     * Used for sharing the CSV files created during the tasting session.
     */
    private fun shareCsvFile(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)

        // Check if the file exists
        if (!file.exists()) {
            viewModelScope.launch {
                sendEvent(Error("CSV file $fileName does not exist."))
            }
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "unipi.msss.foodback.fileprovider",
            file
        )

        viewModelScope.launch {
            sendEvent(ShareCsvFile(uri))
        }
    }

    /**
     * This function checks if the network is available.
     * It checks if the device is connected has the WI-FI connection active.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return (capabilities != null) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }

    /**
     * This function performs the logout operation.
     * It calls the logout method from the tasting use case and updates the state of the view model.
     */
    private fun performLogout() = viewModelScope.launch {
        tastingUseCase.logout()
        updateState(_state.value.copy(showLogoutDialog = false))
        sendEvent(LoggedOut)
    }

    /**
     * This function sends a message to the wearable device to start the sampling process.
     * It handles any exceptions that may occur during the process and sends an error event if needed.
     */
    private fun startWearableSampling() {
        try {
            Sender.sendSamplingMessage(context, false)

        } catch (e: Exception) {
            viewModelScope.launch {
                sendEvent(Error("Error starting wearable sampling: ${e.message}"))
                _state.value.copy(stage = TastingStage.Idle)
                return@launch
            }
        }
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
     * This function sends a message to the wearable device to start the health check.
     * It handles any exceptions that may occur during the process and sends an error event if needed.
     */
    private fun startWearableHealthCheck(){
        try {
            Sender.sendHealthCheck(context)

        } catch (e: Exception) {
            viewModelScope.launch {
                sendEvent(Error("Error starting wearable sampling: ${e.message}"))
                _state.value.copy(stage = TastingStage.Idle)
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
     * This function writes the wearable data to a CSV file.
     * It appends the data to the file if it exists, or creates a new file if it doesn't.
     */
    private fun writeWearableDataToCsv(
        context: Context,
        fileName: String,
        experimentNumber: Int,
        subject: String,
        rating: String,
        data: List<WearableData>
    ) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            if (!file.exists()) {
                file.appendText("experiment,timestamp,value,subject,rating\n")
            }
            val rows = data.joinToString("\n") { entry ->
                "${experimentNumber},${entry.timestamp},${entry.value},${subject},${rating}"
            }
            file.appendText("$rows\n")
            Log.d("CSVWriter", "Successfully wrote to ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("CSVWriter", "Error writing CSV: ${e.message}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverManager?.stop()
        job?.cancel()
        healthCheckJob?.cancel()
    }

}
