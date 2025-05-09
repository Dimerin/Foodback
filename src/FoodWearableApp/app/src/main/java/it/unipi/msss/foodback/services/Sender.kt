package it.unipi.msss.foodback.services

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object Sender {

    fun sendSamplingMessage(context: Context) {
        val client = Wearable.getMessageClient(context)

        val json = JSONObject().apply {
            put("start", true)
        }
        val message = json.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w("Sender", "No connected nodes.")
                } else {
                    for (node in nodes) {
                        try {
                            val result = client.sendMessage(
                                node.id,
                                "/start_sampling",
                                message.toByteArray()
                            ).await()
                            Log.d("Sender", "Sent to ${node.displayName}: $message (Result: $result)")
                        } catch (e: Exception) {
                            Log.e("Sender", "Failed to send to ${node.displayName}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Sender", "Failed to get nodes: ${e.message}", e)
            }
        }
    }
}
