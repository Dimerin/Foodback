package unipi.msss.foodback.home.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager
import unipi.msss.foodback.R
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.home.data.TastingUseCase
import java.io.File
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

    private val _eventsFlow = MutableSharedFlow<TastingNavigationEvents>(replay = 1)
    private val buffer = mutableListOf<SensorData>()
    private var job: Job? = null

    private var serverManager: ServerManager? = null

    companion object {
        private const val TIME_TO_BRING = 5_000L // ms
        private const val TIME_TO_TASTE = 10_000L // ms
    }

    override fun onEvent(event: TastingEvent) {
        when (event) {
            is TastingEvent.SubjectChanged -> {
                _state.value = _state.value.copy(subject = event.value)
                _state.value = _state.value.copy(stage = TastingStage.Idle)

            }

            is TastingEvent.StartProtocol -> {
                if (!isNetworkAvailable(context)) {
                    viewModelScope.launch {
                        _eventsFlow.emit(TastingNavigationEvents.Error("Network permissions are required to start the protocol."))
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

            is TastingEvent.DeleteCsv -> {
                deleteCsvFile()
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
            delay(TIME_TO_BRING)

            // Second beep
            _state.value = _state.value.copy(stage = TastingStage.Recording)
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
                    _eventsFlow.emit(TastingNavigationEvents.Error("No sensor data to save. Check the Mindrove connection."))
                    return@launch
                }
                val rows = buffer.map { sd ->
                    listOf(
                        sd.numberOfMeasurement.toString(),
                        sd.channel1.toString(),
                        sd.channel2.toString(),
                        sd.channel3.toString(),
                        sd.channel4.toString(),
                        sd.channel5.toString(),
                        sd.channel6.toString(),
                        subject,
                        rating.toString()
                    ).joinToString(",")
                }

                val file = File(
                    Environment.getExternalStorageDirectory(),
                    "Foodback/eeg_tasting_data.csv"
                )
                if (!file.exists()) {
                    file.appendText("packet,ch1,ch2,ch3,ch4,ch5,ch6,subject,rating\n")
                }
                file.appendText(rows.joinToString("\n", "\n"))

                _state.value = _state.value.copy(stage = TastingStage.Done)
                _state.value = _state.value.copy(sensorData = emptyList())
                _state.value = _state.value.copy(rating = "")
                _state.value = _state.value.copy(subject = "")
                buffer.clear()
                _eventsFlow.emit(TastingNavigationEvents.Finished)
            } catch (e: Exception) {
                _eventsFlow.emit(TastingNavigationEvents.Error("Failed to save data: ${e.message}"))
            }
        }
    }

    private fun deleteCsvFile() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val file = File(
                    Environment.getExternalStorageDirectory(),
                    "Foodback/eeg_tasting_data.csv"
                )
                if (file.exists()) {
                    if (file.delete()) {
                        _eventsFlow.emit(TastingNavigationEvents.Error("CSV file deleted successfully."))
                    } else {
                        _eventsFlow.emit(TastingNavigationEvents.Error("Failed to delete CSV file."))
                    }
                } else {
                    _eventsFlow.emit(TastingNavigationEvents.Error("CSV file does not exist."))
                }
            } catch (e: Exception) {
                _eventsFlow.emit(TastingNavigationEvents.Error("Failed to delete CSV file: ${e.message}"))
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    override fun onCleared() {
        super.onCleared()
        serverManager?.stop()
        job?.cancel()
    }

    private fun performLogout() = viewModelScope.launch {
        tastingUseCase.logout()
        updateState(_state.value.copy(showLogoutDialog = false))
        sendEvent(TastingNavigationEvents.LoggedOut)
    }

}
