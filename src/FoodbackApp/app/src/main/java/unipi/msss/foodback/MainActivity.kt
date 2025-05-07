package unipi.msss.foodback

import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.net.Uri
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import unipi.msss.foodback.ui.theme.FoodbackTheme
import androidx.core.net.toUri
import java.io.File


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()
        ensureFoodbackFolderExists()



        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return authViewModel.authState.value != AuthState.Loading
                }
            },
        )

        setContent {
            FoodbackTheme {
                val navController = rememberNavController()
                val authState = authViewModel.authState.collectAsStateWithLifecycle().value
                if (authState != AuthState.Loading) {
                    AppNavigator(navController, authState)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 0)
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Grant MANAGE_EXTERNAL_STORAGE permission in settings", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = ("package:$packageName").toUri()
            startActivity(intent)
        }
    }
    private fun ensureFoodbackFolderExists() {
        val foodbackFolder = File(Environment.getExternalStorageDirectory(), "Foodback")
        if (!foodbackFolder.exists()) {
            foodbackFolder.mkdirs()
        }
    }

}
