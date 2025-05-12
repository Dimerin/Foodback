package unipi.msss.foodback.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent

class SamplingMessageListener (
    private val context: Context
) : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "SamplingMessageListener"
        const val PATH = "/start_sampling"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH) {
            Log.d(TAG, "Message Received: ${String(messageEvent.data)}")
            val intent = Intent(context, SamplingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}