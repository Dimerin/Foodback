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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager
import unipi.msss.foodback.R
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.home.data.TastingUseCase
import unipi.msss.foodback.home.ui.TastingNavigationEvents.*
import unipi.msss.foodback.services.HeartRateMessageListener
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
    private val heartRateMessageListener = HeartRateMessageListener()
    private val buffer = mutableListOf<SensorData>()
    private var job: Job? = null
    private var serverManager: ServerManager? = null

    init {
        startHealthCheck()
        Wearable.getMessageClient(context).addListener(heartRateMessageListener) //FIXME
    }

    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch(ioDispatcher) {
            while (true) {
                delay(2000L)

                // Only check when not actively recording
                if (_state.value.stage != TastingStage.Recording) {
                    val isDeviceTransmitting = tryConnectToDevice()
                    _state.value = _state.value.copy(isDeviceConnected = isDeviceTransmitting)
                }
            }
        }
    }

    private suspend fun tryConnectToDevice(): Boolean = withTimeoutOrNull(1500L) { //FIXME
        return@withTimeoutOrNull suspendCancellableCoroutine { cont ->
            var isDataReceived = false
            val tempManager = ServerManager { _ ->
                isDataReceived = true
                cont.resume(true) { _, _, _ -> }
            }

            try {
                tempManager.start()

                // Fallback: if no data comes within the timeout, return false
                viewModelScope.launch {
                    delay(1400L) // Short delay to allow data to arrive
                    if (!isDataReceived && cont.isActive) {
                        cont.resume(false) { _, _, _ -> }
                    }
                }

                // Stop the temp manager once coroutine completes
                cont.invokeOnCancellation {
                    tempManager.stop()
                }

            } catch (_: Exception) {
                tempManager.stop()
                if (cont.isActive) cont.resume(false) { _, _, _ -> }
            }
        }
    } == true


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
                if (!isNetworkAvailable(context) || !state.value.isDeviceConnected) {
                    viewModelScope.launch {
                        sendEvent(Error("Turn on the wifi and connect to your Mindrove device to start the protocol."))
                    }
                    return
                }
                buffer.clear()
                startSession()
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
                deleteCsvFile(context,"eeg_tasting_data.csv")
            }

            is TastingEvent.ShareEEGCsv -> {
                shareCsvFile(context,"eeg_tasting_data.csv")
            }

            is TastingEvent.DeleteEDACsv -> {
                shareCsvFile(context,"eda_data.csv")
            }
            is TastingEvent.DeleteHRCsv -> {
                deleteCsvFile(context,"heart_rate_data.csv")
            }
            is TastingEvent.ShareEDACsv -> {
                shareCsvFile(context,"eda_data.csv")
            }
            is TastingEvent.ShareHRCsv -> {
                shareCsvFile(context,"heart_rate_data.csv")
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
        }
    }

    private fun startSession() {
        serverManager = ServerManager { sensorData: SensorData ->
            if (_state.value.stage == TastingStage.Recording) {
                buffer += sensorData
                _state.value = _state.value.copy(sensorData = buffer.toList())
            }
        }
        serverManager?.start()
    }

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
            startWearableSampling()
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

            serverManager?.stop()
            _state.value = _state.value.copy(stage = TastingStage.AskingRating)
        }
    }

    private fun submitRating(rating: Int, subject: String) {

        viewModelScope.launch(ioDispatcher) {
            try {
                if (buffer.isEmpty()) {
                    sendEvent(Error("No sensor data to save. Check the Mindrove connection."))
                    _state.value = _state.value.copy(stage = TastingStage.Idle)
                    _state.value = _state.value.copy(rating = "")
                    return@launch
                }

                val file = File(context.getExternalFilesDir(null),"eeg_tasting_data.csv")
                val experimentNumber = getExperimentNumber(file)

                val rows = buffer.map { sd ->
                    listOf(
                        sd.numberOfMeasurement.toString(),
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
                    file.appendText("packet,ch1,ch2,ch3,ch4,ch5,ch6,experiment,subject,rating\n")
                }
                file.appendText(rows.joinToString("\n", "\n"))

                collectWearableData(experimentNumber)

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

    private fun getExperimentNumber(file: File): Int {
        if (!file.exists() || file.readText().isBlank()) {
            return 1
        }

        val lastLine = file.readLines().lastOrNull()
        return lastLine?.split(",")?.getOrNull(7)?.toIntOrNull()?.plus(1) ?: 1
    }

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

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return (capabilities != null) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }

    private fun performLogout() = viewModelScope.launch {
        tastingUseCase.logout()
        updateState(_state.value.copy(showLogoutDialog = false))
        sendEvent(LoggedOut)
    }

    private fun startWearableSampling() {
        try {
            Sender.sendSamplingMessage(context)

        } catch (e: Exception) {
            viewModelScope.launch {
                sendEvent(Error("Error starting wearable sampling: ${e.message}"))
                _state.value.copy(stage = TastingStage.Idle)
                return@launch
            }
        }
    }

    private fun collectWearableData(
        experimentNumber: Int
    ) {
        viewModelScope.launch {
            // Collect heart rate data
            HeartRateMessageListener.heartRateFlow.collect { heartRates ->
                if (heartRates.isNotEmpty()) {
                    Log.d("HeartRateViewModel", "Received heart rate data: $heartRates")
                    writeWearableDataToCsv(context, "heart_rate_data.csv", experimentNumber,heartRates)
                }
                else{
                    Log.d("HeartRateViewModel", "No heart rate data received")
                }
            }
        }

        viewModelScope.launch {
            // Collect EDA data
            HeartRateMessageListener.edaFlow.collect { edaValues ->
                if (edaValues.isNotEmpty()) {
                    Log.d("HeartRateViewModel", "Received EDA data: $edaValues")
                    writeWearableDataToCsv(context, "eda_data.csv", experimentNumber,edaValues)
                }
                else{
                    Log.d("HeartRateViewModel", "No EDA data received")
                }
            }
        }
    }

    fun writeWearableDataToCsv(
        context: Context,
        fileName: String,
        experimentNumber: Int,
        data: List<WearableData>
    ) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            if (!file.exists()) {
                file.appendText("experiment,timestamp,value\n")
            }
            val rows = data.joinToString("\n") { entry ->
                "${experimentNumber},${entry.timestamp},${entry.value}"
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
