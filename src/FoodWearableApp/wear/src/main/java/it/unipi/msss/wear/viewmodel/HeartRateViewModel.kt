package it.unipi.msss.wear.viewmodel
import android.content.Context
import androidx.lifecycle.viewModelScope
import it.unipi.msss.wear.model.HeartRateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.Wearable
import it.unipi.msss.wear.model.DataSender
import it.unipi.msss.wear.services.SamplingMessageListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class HeartRateUiState(
    val latestHeartRate: Float? = null,
    val isCollecting: Boolean = false,
    val error: String? = null
)
class HeartRateViewModel(context : Context) : ViewModel() {
    private val heartRateRepository = HeartRateRepository(context.applicationContext)
    private val _uiState = MutableStateFlow(HeartRateUiState())
    val uiState: StateFlow<HeartRateUiState> = _uiState

    init {
        heartRateRepository.start()

        heartRateRepository.latestHeartRate
            .onEach { latestHeartRate ->
                _uiState.value = _uiState.value.copy(latestHeartRate = latestHeartRate)
            }
            .launchIn(viewModelScope)
    }

    fun startCollection(context: Context) {
        if (_uiState.value.isCollecting) {
            return
        }
        heartRateRepository.startCollecting()
        _uiState.value = _uiState.value.copy(isCollecting = true)

        viewModelScope.launch {
            delay(10000)
            stopCollection()
            saveData(context)
        }
    }

    fun stopCollection() {
        heartRateRepository.stopCollecting()
        _uiState.value = _uiState.value.copy(isCollecting = false)
    }

    fun saveData(context: Context) {
        try {
            val collectedHeartRates = heartRateRepository.collectedHeartRates
            val collectedEDA = heartRateRepository.collectedEDA
            val dataToSave =
            DataSender.sendSensorData(context, collectedHeartRates, collectedEDA)
            Log.d("HeartRateViewModel", "Dati salvati: $dataToSave")
            heartRateRepository.clearData()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Errore durante il salvataggio dei dati: ${e.message}")
            Log.e("HeartRateViewModel", "Errore durante il salvataggio dei dati", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartRateRepository.stop()
    }
}
