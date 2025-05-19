package unipi.msss.foodback.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import unipi.msss.foodback.viewmodel.WearableViewModel
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class SensorRepository private constructor(context: Context) {

    companion object {
        const val SENSOR_TYPE_EDA = 65554
        const val TAG = "SensorRepository"

        @Volatile
        private var INSTANCE: SensorRepository? = null

        fun getInstance(context: Context): SensorRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SensorRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var heartRateSensor: Sensor? = null
    private var edaSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null
    private val _collectedHeartRates =
        Collections.synchronizedList(mutableListOf<Pair<Long, Float>>())
    val collectedHeartRates: List<Pair<Long, Float>> get() = _collectedHeartRates

    private val _collectedEDA = Collections.synchronizedList(mutableListOf<Pair<Long, Float>>())
    val collectedEDA: List<Pair<Long, Float>> get() = _collectedEDA

    private val _latestHeartRate = MutableStateFlow<Float?>(null)
    val latestHeartRate: StateFlow<Float?> = _latestHeartRate

    private val _latestEDA = MutableStateFlow<Float?>(null)
    val latestEDA: StateFlow<Float?> = _latestEDA

    private var _isCollectingPrv = AtomicBoolean(false)

    private var _setRedCircle = MutableStateFlow<Boolean>(false)
    var setRedCircle: StateFlow<Boolean> = _setRedCircle

    private val sensorScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        heartRateSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        edaSensor = sensorManager?.getDefaultSensor(SENSOR_TYPE_EDA)

        if (heartRateSensor == null) return
        if (edaSensor == null) {
            Log.e(TAG, "eda sensor not found")
        }

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_HEART_RATE -> {
                            Log.d("HR", "[${_isCollectingPrv.get()}]")
                            val heartRate = it.values[0]
                            if (_isCollectingPrv.get()) {
                                _collectedHeartRates.add(
                                    Pair(System.currentTimeMillis(), heartRate)
                                )
                            }
                            _latestHeartRate.value = heartRate
                        }

                        SENSOR_TYPE_EDA -> {
                            val edaValue = it.values[0]
                            if (_isCollectingPrv.get()) {
                                _collectedEDA.add(
                                    Pair(System.currentTimeMillis(), edaValue)
                                )
                            }
                            _latestEDA.value = edaValue
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorScope.launch {
            // Launch a task on a secondary thread
            sensorManager?.registerListener(
                sensorListener,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )

            edaSensor?.let {
                sensorManager?.registerListener(
                    sensorListener,
                    it,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            }
        }
    }

    fun stop() {
        sensorListener?.let {
            sensorManager?.unregisterListener(it)
        }
    }

    fun startCollecting() {
        _isCollectingPrv.set(true)
        _setRedCircle.value = true
    }

    fun stopCollecting() {
        _isCollectingPrv.set(false)
        _setRedCircle.value = false
    }

    fun clearData() {
        _collectedHeartRates.clear()
        _collectedEDA.clear()
        _latestHeartRate.value = null
        _latestEDA.value = null
    }

    fun saveData(context: Context) {
        try {
            // Crea copie sicure dei dati
            var collectedHeartRates = _collectedHeartRates.toList()
            var collectedEDA = _collectedEDA.toList()

            // Aggiungi l'ultimo valore di EDA se non ci sono dati
            if (collectedEDA.isEmpty()) {
                val latestEda = _latestEDA.value ?: 0f // Assicurati che ci sia un valore di fallback
                collectedEDA = collectedEDA.toMutableList().apply {
                    add(Pair(System.currentTimeMillis(), latestEda))
                }
            }

            // Aggiungi l'ultimo valore di HeartRate se non ci sono dati
            if (collectedHeartRates.isEmpty()) {
                val latestHr = _latestHeartRate.value ?: 0f // Assicurati che ci sia un valore di fallback
                collectedHeartRates = collectedHeartRates.toMutableList().apply {
                    add(Pair(System.currentTimeMillis(), latestHr))
                }
            }

            // Invia i dati ai destinatari (esempio, a un server o a un database)
            val dataToSave = DataSender.sendSensorData(context, collectedHeartRates, collectedEDA)

            // Log del dato salvato
            Log.d(TAG, "Saved Data: $dataToSave")

            // Pulisce i dati raccolti
            clearData()
        } catch (e: Exception) {
            // Gestisci gli errori e aggiorna lo stato con il messaggio di errore
            Log.e(TAG, "Error while saving data", e)
            // Opzionale: usa un altro flusso di stato per gestire gli errori nella UI
        }
    }

}

