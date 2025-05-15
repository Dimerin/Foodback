package unipi.msss.foodback.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import org.json.JSONObject
import unipi.msss.foodback.model.DataSender

class SamplingMessageListener (
    private val context: Context
) : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "SamplingMessageListener"
        const val PATH_START = "/start_sampling"
        const val PATH_HEALTH = "/check_health"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_START -> {
                Log.d(TAG, "Start sampling: ${String(messageEvent.data)}")

                val jsonStr = String(messageEvent.data)
                val json = JSONObject(jsonStr)
                val isInference = json.optBoolean("isInference", false)

                val intent = Intent(context, SamplingService::class.java).apply {
                    putExtra("isInference", isInference)
                }

                ContextCompat.startForegroundService(context, intent)
            }

            PATH_HEALTH -> {
                Log.d(TAG, "Checking health: ${String(messageEvent.data)}")
                DataSender.sendHealtStatus(context)
            }
        }
    }
}