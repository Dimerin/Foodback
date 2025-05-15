package unipi.msss.foodback

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import unipi.msss.foodback.services.SensorService
import unipi.msss.foodback.ui.WearableScreen
import unipi.msss.foodback.ui.theme.FoodbackTheme
import unipi.msss.foodback.viewmodel.WearableViewModel
import unipi.msss.foodback.viewmodel.WearableViewModelFactory

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG : String = "MainActivity"
    }

    private val wearableViewModel: WearableViewModel by viewModels {
        WearableViewModelFactory(applicationContext)
    }

    private val requiredPermissions = buildList {
        add(Manifest.permission.BODY_SENSORS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val denied = results.filterValues { !it }.keys
            if (denied.isNotEmpty()) {
                Toast.makeText(this, "Permissions denied: $denied", Toast.LENGTH_LONG).show()
            }else{
                startService()
            }
            showContent()
        }

    override fun onStart() {
        super.onStart()
        if (arePermissionsGranted()) {
            startService()
            showContent()
        } else {
            requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun arePermissionsGranted(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun showContent() {
        setContent {
            FoodbackTheme {
                WearableScreen(viewModel = wearableViewModel)
            }
        }
    }

    private fun startService() {
        Log.d(TAG, "${SensorService.isRunning}")
        if(!SensorService.isRunning) {
            Log.d(TAG, "Launching service")
            val serviceIntent = Intent(this, SensorService::class.java)
            startForegroundService(serviceIntent)
        }
    }
}
