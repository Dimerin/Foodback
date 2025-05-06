package it.unipi.msss.wear.model

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*
import kotlin.math.pow
import kotlin.math.sqrt

class HeartRateRepository(private val context: Context) {

    private var sensorManager: SensorManager? = null
    private var heartRateSensor: Sensor? = null
    private var heartRateListener: SensorEventListener? = null

    suspend fun collectHeartRateData(durationSeconds: Int = 10): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            val collectedData = mutableListOf<Float>()

            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

            if (heartRateSensor == null) return@withContext null

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        if (it.sensor.type == Sensor.TYPE_HEART_RATE) {
                            collectedData.add(it.values[0])
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(
                listener,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )

            delay(durationSeconds * 1000L)

            sensorManager.unregisterListener(listener)

            if (collectedData.isNotEmpty()) {
                val avg = collectedData.average()
                val std = sqrt(collectedData.map { (it - avg).pow(2) }.average())
                Pair(avg, std)
            } else {
                null
            }
        }
    }
}
