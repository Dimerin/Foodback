package unipi.msss.foodback.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import unipi.msss.foodback.R
import unipi.msss.foodback.model.SensorRepository
import unipi.msss.foodback.util.createNotificationChannel

class SensorService : Service() {
    companion object {
        const val TAG : String = "SensorService"
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(applicationContext, "sensor_channel")
        Log.d(TAG, "Service started")

        val repo = SensorRepository.getInstance(applicationContext)
        isRunning = true
        repo.start()

        val notification = NotificationCompat.Builder(this, "sensor_channel")
            .setContentTitle("Starting measurements")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service terminated")
        val repo = SensorRepository.getInstance(applicationContext)
        repo.stop()
        isRunning = false
    }
}
