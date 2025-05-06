package it.unipi.msss.wear.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unipi.msss.wear.model.DataSender
import it.unipi.msss.wear.model.HeartRateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HeartRateState(
    val isCollecting: Boolean = false,
    val avg: Double? = null,
    val stdev: Double? = null,
    val error: String? = null
)

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HeartRateState())
    val uiState: StateFlow<HeartRateState> = _uiState

    private val repository = HeartRateRepository(application)

    fun startCollection() {
        if (_uiState.value.isCollecting) return

        _uiState.value = HeartRateState(isCollecting = true)

        viewModelScope.launch {
            val result = repository.collectHeartRateData()
            if (result != null) {
                val (avg, stdev) = result
                _uiState.value = HeartRateState(
                    isCollecting = false,
                    avg = avg,
                    stdev = stdev
                )
                DataSender.sendHeartRateStats(getApplication(), avg, stdev)
            } else {
                _uiState.value = HeartRateState(
                    isCollecting = false,
                    error = "Nessun dato raccolto"
                )
            }
        }
    }
}
