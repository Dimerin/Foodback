package unipi.msss.foodback.services

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject


data class WearableData(val timestamp: Long, val value: Float)


class HeartRateMessageListener : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "HeartRateListener"
        const val PATH = "/sensor_series"

        val heartRateFlow = MutableSharedFlow<List<WearableData>>()
        val edaFlow = MutableSharedFlow<List<WearableData>>()
        // Emitting EDA list
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH) {
            val message = String(messageEvent.data)
            Log.d(TAG, "Received Message: $message")

            try {
                val json = JSONObject(message)
                val heartRateJsonArray = json.getJSONArray("heart_rate")
                val edaJsonArray = json.getJSONArray("eda")

                val heartRateData = List(heartRateJsonArray.length()) { i ->
                    val entry = heartRateJsonArray.getJSONObject(i)
                    WearableData(
                        timestamp = entry.getLong("timestamp"),
                        value = entry.getDouble("value").toFloat()
                    )
                }

                val edaData = List(edaJsonArray.length()) { i ->
                    val entry = edaJsonArray.getJSONObject(i)
                    WearableData(
                        timestamp = entry.getLong("timestamp"),
                        value = entry.getDouble("value").toFloat()
                    )
                }

                CoroutineScope(Dispatchers.Main).launch {
                    heartRateFlow.emit(heartRateData)
                    edaFlow.emit(edaData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during parsing of JSON message: ${e.message}", e)
            }
        }
    }

}
