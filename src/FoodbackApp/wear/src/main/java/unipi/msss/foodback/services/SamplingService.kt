package unipi.msss.foodback.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import unipi.msss.foodback.R
import unipi.msss.foodback.model.SensorRepository
import unipi.msss.foodback.util.createNotificationChannel
import unipi.msss.foodback.viewmodel.WearableViewModel

class SamplingService : Service() {

    private lateinit var viewModel: WearableViewModel

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(applicationContext)
        showNotification()
        viewModel = WearableViewModel(applicationContext)
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this, "sampling_channel")
            .setContentTitle("Campionamento in corso")
            .setContentText("Sto raccogliendo i dati dai sensori...")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)  // Imposta l'icona
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        viewModel.startCollection(applicationContext)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null
}

