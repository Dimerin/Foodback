package unipi.msss.foodback

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.Wearable
import unipi.msss.foodback.services.SamplingMessageListener
import unipi.msss.foodback.ui.WearableScreen
import unipi.msss.foodback.ui.theme.FoodbackTheme
import unipi.msss.foodback.viewmodel.WearableViewModel
import unipi.msss.foodback.viewmodel.WearableViewModelFactory

class MainActivity : ComponentActivity() {
    private val wearableViewModel : WearableViewModel by viewModels {
        WearableViewModelFactory(applicationContext)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                setContent {
                    FoodbackTheme {
                        WearableScreen(viewModel = wearableViewModel)
                    }
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    private var samplingMessageListener: SamplingMessageListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                setContent {
                    FoodbackTheme {
                        WearableScreen(viewModel = wearableViewModel)
                    }
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }
    }
}