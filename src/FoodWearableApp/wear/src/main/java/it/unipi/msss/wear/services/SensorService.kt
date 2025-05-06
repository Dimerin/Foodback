package it.unipi.msss.wear.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import it.unipi.msss.wear.model.DataSender
import it.unipi.msss.wear.model.HeartRateRepository
import kotlinx.coroutines.*

class SensorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SensorService", "Starting 10-sec heart rate sampling...")

        serviceScope.launch {
            val repository = HeartRateRepository(this@SensorService)
            val result = repository.collectHeartRateData()

            result?.let { (avg, std) ->
                Log.d("SensorService", "Collected data - Avg: $avg, Stdev: $std")
                DataSender.sendHeartRateStats(this@SensorService, avg, std)
            } ?: Log.w("SensorService", "No heart rate data collected.")

            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
