package it.unipi.msss.wear.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import it.unipi.msss.wear.model.HeartRateRepository
import kotlinx.coroutines.*

class SensorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val heartRateSensor = HeartRateRepository(this@SensorService)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SensorService", "Starting heart rate sampling...")

        serviceScope.launch {
            Log.d("SensorService", "Start collecting data")
            heartRateSensor.start()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        heartRateSensor.stop()
        serviceScope.cancel()
    }
}

