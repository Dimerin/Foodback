package unipi.msss.foodback.services

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import unipi.msss.foodback.viewmodel.WearableViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SamplingMessageListener (
    private val context: Context,
    private val viewModel: WearableViewModel
) : MessageClient.OnMessageReceivedListener {

    companion object {
        const val TAG = "SamplingMessageListener"
        const val PATH = "/start_sampling"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "$messageEvent")
        if (messageEvent.path == PATH) {
            val message = String(messageEvent.data)
            Log.d(TAG, "Receive sampling message: $message")
            CoroutineScope(Dispatchers.Main).launch {
                viewModel.startCollection(context)
            }
        }
    }
}