package unipi.msss.foodback.viewmodel
import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import androidx.lifecycle.ViewModel
import unipi.msss.foodback.model.DataSender
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import unipi.msss.foodback.model.SensorRepository

data class WearableUiState(
    val latestHeartRate: Float? = null,
    val latestEda: Float? = null,
    val isCollecting: Boolean = false,
    val error: String? = null
)
class WearableViewModel(context : Context) : ViewModel() {

    companion object {
        const val TAG = "WearableViewModel"
    }

    private val sensorRepository = SensorRepository.getInstance(context.applicationContext)
    private val _uiState = MutableStateFlow(WearableUiState())
    val uiState: StateFlow<WearableUiState> = _uiState

    init {
        Log.d(TAG,"ENTRO In init")
        //sensorRepository.start()

        sensorRepository.latestHeartRate
            .onEach { latestHeartRate ->
                _uiState.value = _uiState.value.copy(latestHeartRate = latestHeartRate)
            }
            .launchIn(viewModelScope)
        sensorRepository.latestEDA
            .onEach { latestEda ->
                _uiState.value = _uiState.value.copy(latestEda = latestEda)
            }
            .launchIn(viewModelScope)
        sensorRepository.setRedCircle
            .onEach { setRedCircle ->
                _uiState.value = _uiState.value.copy(isCollecting = setRedCircle)
            }
            .launchIn(viewModelScope)
    }

    fun startCollection(context: Context) {
        if (_uiState.value.isCollecting) {
            return
        }
        sensorRepository.startCollecting()
        _uiState.value = _uiState.value.copy(isCollecting = true)
    }

    fun stopCollection() {
        sensorRepository.stopCollecting()
        _uiState.value = _uiState.value.copy(isCollecting = false)
    }

    fun saveData(context: Context) {
        try {
            var collectedHeartRates = sensorRepository.collectedHeartRates
            var collectedEDA = sensorRepository.collectedEDA

            if (collectedEDA.isEmpty()) {
                val latestEda = _uiState.value.latestEda ?: 0f
                collectedEDA = collectedEDA.toMutableList().apply {
                    add(Pair(System.currentTimeMillis(), latestEda))
                }
            }

            if (collectedHeartRates.isEmpty()) {
                val latestHr = _uiState.value.latestHeartRate ?: 0f
                collectedHeartRates = collectedHeartRates.toMutableList().apply {
                    add(Pair(System.currentTimeMillis(), latestHr))
                }
            }

            val dataToSave = DataSender
                .sendSensorData(context, collectedHeartRates, collectedEDA)

            Log.d(TAG, "Saved Data: $dataToSave")
            sensorRepository.clearData()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Error while saving data: ${e.message}")
            Log.e(TAG, "Error while saving data", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
