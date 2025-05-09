package it.unipi.msss.wear.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HeartRateRepository(private val context: Context) {

    companion object {
        const val SENSOR_TYPE_EDA = 65554
        const val TAG = "HeartRateRepository"
        const val SENSOR_THREAD = "Sensor Thread"
    }

    private var sensorManager: SensorManager? = null
    private var heartRateSensor: Sensor? = null
    private var edaSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null

    private val _collectedHeartRates = mutableListOf<Float>()
    val collectedHeartRates: List<Float> get() = _collectedHeartRates

    private val _collectedEDA = mutableListOf<Float>()
    val collectedEDA: List<Float> get() = _collectedEDA

    private val _latestHeartRate = MutableStateFlow<Float?>(null)
    val latestHeartRate: StateFlow<Float?> = _latestHeartRate

    private val _latestEDA = MutableStateFlow<Float?>(null)
    val latestEDA: StateFlow<Float?> = _latestEDA

    private var _isCollecting : Boolean = false

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
                            if (_isCollecting) _collectedHeartRates.add(heartRate)
                            _latestHeartRate.value = heartRate
                        }
                        SENSOR_TYPE_EDA -> { // EDA Sensor
                            val edaValue = it.values[0]
                            if (_isCollecting) _collectedEDA.add(edaValue)
                            _latestEDA.value = edaValue
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val handlerThread = HandlerThread(SENSOR_THREAD)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        sensorManager?.registerListener(
            sensorListener,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_FASTEST,
            handler
        )

        if (edaSensor != null) {
            sensorManager?.registerListener(
                sensorListener,
                edaSensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                handler
            )
        }else{Log.e(TAG,"Eda sensor is null")}
    }

    fun stop() {
        sensorListener?.let {
            sensorManager?.unregisterListener(it)
        }
    }

    fun startCollecting(){
        _isCollecting = true
    }

    fun stopCollecting(){
        _isCollecting = false
    }

    fun clearData() {
        _collectedHeartRates.clear()
        _collectedEDA.clear()
        _latestHeartRate.value = null
        _latestEDA.value = null
    }
}

