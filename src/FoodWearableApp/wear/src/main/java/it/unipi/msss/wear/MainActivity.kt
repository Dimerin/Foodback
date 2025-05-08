package it.unipi.msss.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import it.unipi.msss.wear.ui.HeartRateScreen

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permesso concesso", Toast.LENGTH_SHORT).show()
                setContent {
                    HeartRateScreen()
                }
            } else {
                Toast.makeText(this, "Permesso negato", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Controlla se il permesso è già stato concesso
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Il permesso è già concesso, procedi con la logica
                setContent {
                    HeartRateScreen()
                }
            }
            else -> {
                // Chiedi il permesso
                requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }
    }
}