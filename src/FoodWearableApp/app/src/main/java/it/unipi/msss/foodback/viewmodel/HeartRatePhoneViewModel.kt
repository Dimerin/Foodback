package it.unipi.msss.foodback.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unipi.msss.foodback.services.HeartRateMessageListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PhoneHeartRateState(
    val lastAvg: Double? = null,
    val lastStdev: Double? = null
)

class HeartRatePhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhoneHeartRateState())
    val uiState: StateFlow<PhoneHeartRateState> = _uiState

    init {
        viewModelScope.launch {
            HeartRateMessageListener.heartRateFlow.collect { (avg, stdev) ->
                _uiState.value = PhoneHeartRateState(lastAvg = avg, lastStdev = stdev)
            }
        }
    }
}
