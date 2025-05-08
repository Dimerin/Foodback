package it.unipi.msss.wear.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HeartRateRepository(private val context: Context) {

    private var sensorManager: SensorManager? = null
    private var heartRateSensor: Sensor? = null
    private var heartRateListener: SensorEventListener? = null

    private val _collectedData = mutableListOf<Float>()
    val collectedData: List<Float> get() = _collectedData

    private val _latestHeartRate = MutableStateFlow<Float?>(null)
    val latestHeartRate: StateFlow<Float?> = _latestHeartRate

    private var _isCollecting : Boolean = false

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (heartRateSensor == null) return

        heartRateListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_HEART_RATE) {
                        val heartRate = it.values[0]
                        if(_isCollecting)
                            _collectedData.add(heartRate)
                        _latestHeartRate.value = heartRate
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(
            heartRateListener,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    fun stop() {
        heartRateListener?.let {
            sensorManager?.unregisterListener(it)
        }
    }

    fun startCollecting(){
        _isCollecting = true
    }

    fun stopCollecting(){
        _isCollecting = true
    }

    fun clearData() {
        _collectedData.clear()
        _latestHeartRate.value = null
    }
}

