package unipi.msss.foodback.services

import android.app.Service
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import unipi.msss.foodback.R
import unipi.msss.foodback.util.createNotificationChannel
import unipi.msss.foodback.viewmodel.WearableViewModel

class SamplingService : Service() {

    private lateinit var viewModel: WearableViewModel
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(applicationContext)
        viewModel = WearableViewModel(applicationContext)
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
        viewModel.startCollection(applicationContext)
        serviceJob = CoroutineScope(Dispatchers.Default).launch {
            delay(10000)  // wait for 10 seconds
            viewModel.stopCollection()
            viewModel.saveData(applicationContext)
            stopSelf()    // Stop the service
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Sampling Service", "Terminated")
    }
    override fun onBind(intent: Intent?) = null
}

