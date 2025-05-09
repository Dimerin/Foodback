package it.unipi.msss.wear.model

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

object DataSender {

    fun List<Pair<Long, Float>>.toJsonArray(): JSONArray {
        val jsonArray = JSONArray()
        for ((timestamp, value) in this) {
            val obj = JSONObject()
            obj.put("timestamp", timestamp)
            obj.put("value", value)
            jsonArray.put(obj)
        }
        return jsonArray
    }

    fun sendSensorData(context: Context, heartRates: List<Pair<Long, Float>>, edaValues: List<Pair<Long, Float>>) {
        val client = Wearable.getMessageClient(context)

        val json = JSONObject().apply {
            put("heart_rate", heartRates.toJsonArray())
            put("eda", edaValues.toJsonArray())
        }
        val message = json.toString()

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
                                "/sensor_series",
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

