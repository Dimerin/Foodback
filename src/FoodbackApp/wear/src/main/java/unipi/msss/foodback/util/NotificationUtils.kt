package unipi.msss.foodback.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService

fun createNotificationChannel(context: Context, channelId: String) {
    val channel = NotificationChannel(
        channelId,
        "Sensors sampling",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}
