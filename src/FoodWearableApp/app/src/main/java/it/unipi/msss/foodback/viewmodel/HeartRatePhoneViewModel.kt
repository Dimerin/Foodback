package it.unipi.msss.foodback.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unipi.msss.foodback.services.HeartRateMessageListener
import it.unipi.msss.foodback.services.Sender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

data class PhoneHeartRateState(
    val lastAvg: Double? = null,
    val lastStdev: Double? = null,
    val edaAvg: Double? = null,
    val edaStdev: Double? = null
)

class HeartRatePhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhoneHeartRateState())
    val uiState: StateFlow<PhoneHeartRateState> = _uiState

    init {
        viewModelScope.launch {
            HeartRateMessageListener.heartRateFlow.collect { heartRates ->
                if (heartRates.isNotEmpty()) {
                    val avg = heartRates.average()
                    val stdev = sqrt(heartRates.map { (it - avg).pow(2) }.average())
                    _uiState.value = _uiState.value.copy(lastAvg = avg, lastStdev = stdev)
                }
            }
        }

        viewModelScope.launch {
            HeartRateMessageListener.edaFlow.collect { edaValues ->
                if (edaValues.isNotEmpty()) {
                    val avg = edaValues.average()
                    val stdev = sqrt(edaValues.map { (it - avg).pow(2) }.average())
                    _uiState.value = _uiState.value.copy(edaAvg = avg, edaStdev = stdev)
                }
            }
        }

        try {
            Sender.sendSamplingMessage(application)
            Log.d("HeartPhoneViewModel", "Start sampling inviato")
        } catch (e: Exception) {
            Log.e("HeartRateViewModel", "Errore durante invio", e)
        }
    }
}
