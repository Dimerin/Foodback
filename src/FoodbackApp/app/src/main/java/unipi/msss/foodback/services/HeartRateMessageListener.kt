package unipi.msss.foodback.services

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class HeartRateMessageListener : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "HeartRateListener"
        const val PATH = "/sensor_series"

        val heartRateFlow = MutableSharedFlow<List<Float>>()  // Emitting heart rate list
        val edaFlow = MutableSharedFlow<List<Float>>()         // Emitting EDA list
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH) {
            val message = String(messageEvent.data)
            Log.d(TAG, "Received Message: $message")

            try {
                val json = JSONObject(message)
                val heartRateJsonArray = json.getJSONArray("heart_rate")
                val edaJsonArray = json.getJSONArray("eda")

                val heartRates = List(heartRateJsonArray.length()) { i ->
                    heartRateJsonArray.getDouble(i).toFloat()
                }

                val edaValues = List(edaJsonArray.length()) { i ->
                    edaJsonArray.getDouble(i).toFloat()
                }

                CoroutineScope(Dispatchers.Main).launch {
                    heartRateFlow.emit(heartRates)
                    edaFlow.emit(edaValues)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore during parsing of json message: ${e.message}", e)
            }
        }
    }
}
