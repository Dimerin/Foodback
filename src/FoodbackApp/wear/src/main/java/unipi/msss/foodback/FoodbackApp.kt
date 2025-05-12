package unipi.msss.foodback

import android.app.Application
import com.google.android.gms.wearable.Wearable
import unipi.msss.foodback.services.SamplingMessageListener
import unipi.msss.foodback.viewmodel.WearableViewModel

class FoodbackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val listener = SamplingMessageListener(applicationContext)
        Wearable.getMessageClient(this).addListener(listener)
    }
}