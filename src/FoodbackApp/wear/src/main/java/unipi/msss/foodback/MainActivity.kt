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
import unipi.msss.foodback.ui.HeartRateScreen
import unipi.msss.foodback.ui.theme.FoodbackTheme
import unipi.msss.foodback.viewmodel.HeartRateViewModel
import unipi.msss.foodback.viewmodel.HeartRateViewModelFactory

class MainActivity : ComponentActivity() {
    private val heartRateViewModel : HeartRateViewModel by viewModels {
        HeartRateViewModelFactory(applicationContext)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permesso concesso", Toast.LENGTH_SHORT).show()
                setContent {
                    HeartRateScreen(viewModel = heartRateViewModel)
                }
            } else {
                Toast.makeText(this, "Permesso negato", Toast.LENGTH_SHORT).show()
            }
        }
    private var samplingMessageListener: SamplingMessageListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        samplingMessageListener = SamplingMessageListener(applicationContext, heartRateViewModel)
        Wearable.getMessageClient(this).addListener(samplingMessageListener!!)

        // Controlla se il permesso è già stato concesso
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Il permesso è già concesso, procedi con la logica
                setContent {
                    HeartRateScreen(viewModel = heartRateViewModel)
                }
            }
            else -> {
                // Chiedi il permesso
                requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }

        setContent {
            FoodbackTheme {
                HeartRateScreen()
            }
        }
    }
}