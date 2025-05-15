package unipi.msss.foodback.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import unipi.msss.foodback.R
import unipi.msss.foodback.model.SensorRepository
import unipi.msss.foodback.util.createNotificationChannel
import unipi.msss.foodback.viewmodel.WearableViewModel
import androidx.core.content.edit

class SamplingService : Service() {

    companion object {
        const val TAG : String = "SamplingService"
    }

    private var serviceJob: Job? = null

    private lateinit var sensorRepository :SensorRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(applicationContext,"sampling_channel")
        sensorRepository = SensorRepository.getInstance(applicationContext)
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, "sampling_channel")
            .setContentTitle("Sampling in progress")
            .setContentText("I'm collecting data from the sensors...")
            .setSmallIcon(R.drawable.notify_icon)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()

        val isInference = intent?.getBooleanExtra("isInference", false) ?: false
        Log.d(TAG, "isInference: $isInference")
        val samplingTime = if (isInference) 2000L else 10000L

        sensorRepository.startCollecting()

        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            delay(samplingTime)
            sensorRepository.stopCollecting()
            sensorRepository.saveData(applicationContext)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Terminated")
    }
    override fun onBind(intent: Intent?) = null
}

