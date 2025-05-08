package it.unipi.msss.foodback.services

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class HeartRateMessageListener : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "HeartRateListener"
        const val PATH = "/sensor_series"

        val heartRateFlow = MutableSharedFlow<Pair<Double, Double>>()  // (avg, stdev)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "$messageEvent")
        if (messageEvent.path == PATH) {
            val message = String(messageEvent.data)
            Log.d(TAG, "Messaggio ricevuto: $message")

            val regex = Regex("avg:([0-9.]+);stdev:([0-9.]+)")
            val match = regex.find(message)
            if (match != null) {
                val avg = match.groupValues[1].toDouble()
                val stdev = match.groupValues[2].toDouble()

                CoroutineScope(Dispatchers.Main).launch {
                    heartRateFlow.emit(avg to stdev)
                }
            } else {
                Log.e(TAG, "Formato messaggio non valido: $message")
            }
        }
    }
}
