package it.unipi.msss.foodback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import it.unipi.msss.foodback.ui.HeartRatePhoneScreen
import com.google.android.gms.wearable.Wearable
import it.unipi.msss.foodback.services.HeartRateMessageListener

class MainActivity : ComponentActivity() {
    private val heartRateMessageListener = HeartRateMessageListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeartRatePhoneScreen()
        }

        // Registrazione listener
        Wearable.getMessageClient(this).addListener(heartRateMessageListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(heartRateMessageListener)
    }

    override fun onResume() {
        super.onResume()
        // Aggiungi il listener quando l'attività è visibile e in primo piano
        Wearable.getMessageClient(this).addListener(heartRateMessageListener)
    }

    override fun onPause() {
        super.onPause()
        // Rimuovi il listener quando l'attività non è visibile
        Wearable.getMessageClient(this).removeListener(heartRateMessageListener)
    }

}
