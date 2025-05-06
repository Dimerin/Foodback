package it.unipi.msss.wear.model

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object DataSender {

    fun sendHeartRateStats(context: Context, avg: Double, std: Double) {
        val client = Wearable.getMessageClient(context)
        val message = "avg:$avg;stdev:$std"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w("DataSender", "No connected nodes.")
                } else {
                    for (node in nodes) {
                        try {
                            val result = client.sendMessage(
                                node.id,
                                "/sensor_data",
                                message.toByteArray()
                            ).await()
                            Log.d("DataSender", "Sent to ${node.displayName}: $message (Result: $result)")
                        } catch (e: Exception) {
                            Log.e("DataSender", "Failed to send to ${node.displayName}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DataSender", "Failed to get nodes: ${e.message}", e)
            }
        }
    }
}
