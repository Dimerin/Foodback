package it.unipi.msss.wear.viewmodel
import androidx.lifecycle.viewModelScope
import it.unipi.msss.wear.model.HeartRateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class HeartRateUiState(
    val latestHeartRate: Float? = null,
    val isCollecting: Boolean = false,
    val error: String? = null
)
class HeartRateViewModel(private val heartRateRepository: HeartRateRepository) : ViewModel() {
    private val heartRateSensor = HeartRateRepository()
    private val _uiState = MutableStateFlow(HeartRateUiState())
    val uiState: StateFlow<HeartRateUiState> = _uiState

    init {
        heartRateRepository.latestHeartRate.onEach { latestHeartRate ->
                _uiState.value = _uiState.value.copy(latestHeartRate = latestHeartRate)
            }.launchIn(viewModelScope)
    }

    fun startCollection() {
        heartRateRepository.startCollecting()
        _uiState.value = _uiState.value.copy(isCollecting = true)
    }

    fun stopCollection() {
        heartRateRepository.stopCollecting()
        _uiState.value = _uiState.value.copy(isCollecting = false)
    }

    fun saveData() {
        val dataToSave = heartRateRepository.collectedData
        // Logica per salvare i dati (ad esempio, in un database o file)
        // Puoi aggiungere il codice per salvare o processare i dati
        Log.d("HeartRateViewModel", "Dati salvati: $dataToSave")
        heartRateRepository.clearData() // Pulisce i dati dopo averli salvati
    }
}
