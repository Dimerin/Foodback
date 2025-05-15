package unipi.msss.foodback

import android.app.Application
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.Wearable
import unipi.msss.foodback.model.SensorRepository
import unipi.msss.foodback.services.SamplingMessageListener
import unipi.msss.foodback.services.SensorService
import unipi.msss.foodback.viewmodel.WearableViewModel

class FoodbackApp : Application() {
    companion object {
        const val TAG : String = "FoodbackApp"
    }
    override fun onCreate() {
        super.onCreate()
        val listener = SamplingMessageListener(applicationContext)
        Wearable.getMessageClient(this).addListener(listener)
        Log.d(TAG, "Application starting")
    }
}