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


class WearableMessageListener : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "WearableMessageListener"
        const val PATH_SAMPLING = "/sensor_series"
        const val PATH_HEALTH = "/check_health"

        val heartRateFlow = MutableSharedFlow<List<WearableData>>()
        val edaFlow = MutableSharedFlow<List<WearableData>>()
        val healthCheckFlow = MutableSharedFlow<Unit>()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_SAMPLING -> {
                val message = String(messageEvent.data)
                Log.d(TAG, "Received sensor data: $message")

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
                    Log.e(TAG, "Errore nel parsing JSON: ${e.message}", e)
                }
            }

            PATH_HEALTH -> {
                Log.d(TAG, "Ricevuto messaggio di health check")
                CoroutineScope(Dispatchers.Main).launch {
                    healthCheckFlow.emit(Unit)
                }
            }

            else -> {
                Log.w(TAG, "Path sconosciuto: ${messageEvent.path}")
            }
        }
    }

}
