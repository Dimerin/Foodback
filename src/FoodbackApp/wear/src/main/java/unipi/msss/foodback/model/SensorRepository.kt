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
import java.util.Collections

class SensorRepository(private val context: Context) {

    companion object {
        const val SENSOR_TYPE_EDA = 65554
        const val TAG = "SensorRepository"
    }

    private var sensorManager: SensorManager? = null
    private var heartRateSensor: Sensor? = null
    private var edaSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null
    private val _collectedHeartRates = Collections.synchronizedList(mutableListOf<Pair<Long, Float>>())
    val collectedHeartRates: List<Pair<Long, Float>> get() = _collectedHeartRates

    private val _collectedEDA = Collections.synchronizedList(mutableListOf<Pair<Long, Float>>())
    val collectedEDA: List<Pair<Long, Float>> get() = _collectedEDA

    private val _latestHeartRate = MutableStateFlow<Float?>(null)
    val latestHeartRate: StateFlow<Float?> = _latestHeartRate

    private val _latestEDA = MutableStateFlow<Float?>(null)
    val latestEDA: StateFlow<Float?> = _latestEDA

    @Volatile
    private var _isCollecting: Boolean = false

    private val sensorScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
                            val heartRate = it.values[0]
                            if (_isCollecting) _collectedHeartRates.add(
                                Pair(System.currentTimeMillis(), heartRate))
                            _latestHeartRate.value = heartRate
                        }
                        SENSOR_TYPE_EDA -> {
                            val edaValue = it.values[0]
                            if (_isCollecting) _collectedEDA.add(
                                Pair(System.currentTimeMillis(), edaValue))
                            _latestEDA.value = edaValue
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorScope.launch {
            // Lancia un task su un thread secondario
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
        _isCollecting = true
    }

    fun stopCollecting() {
        _isCollecting = false
    }

    fun clearData() {
        _collectedHeartRates.clear()
        _collectedEDA.clear()
        _latestHeartRate.value = null
        _latestEDA.value = null
    }
}

