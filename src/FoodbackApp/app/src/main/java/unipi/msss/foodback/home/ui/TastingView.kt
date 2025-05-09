package unipi.msss.foodback.home.ui


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import unipi.msss.foodback.R
import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.ui.theme.FoodbackPreview
import unipi.msss.foodback.ui.theme.FoodbackTheme


@Composable
fun TastingView(
    viewModel: TastingViewModel = hiltViewModel(),
    onFinished: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    TastingScreen(state = state, onEvent = { viewModel.onEvent(it) })

    ViewEvent(viewModel.eventsFlow) { event ->
        when (event) {
            is TastingNavigationEvents.Finished -> onFinished()
            is TastingNavigationEvents.Error -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            is TastingNavigationEvents.LoggedOut -> {
                onLoggedOut()
                Toast.makeText(context, "Logged out", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TastingScreen(
    state: TastingState = TastingState(),
    onEvent: (TastingEvent) -> Unit = {},
) {


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasting Protocol", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onEvent.invoke(TastingEvent.ShowLogoutDialog) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                composition = rememberLottieComposition(
                    spec = LottieCompositionSpec.RawRes(
                        when {
                            state.isBringingToMouth -> R.raw.mouth_anim
                            state.isRecording -> R.raw.eeg_anim
                            state.showRatingInput -> R.raw.star_anim
                            state.isFinished -> R.raw.end_anim
                            else -> R.raw.data_anim
                        }
                    )
                ).value,
                iterations = if (state.isFinished) 1 else LottieConstants.IterateForever
            )

            OutlinedTextField(
                value = state.subject,
                onValueChange = { onEvent(TastingEvent.SubjectChanged(it)) },
                label = { Text("Subject (e.g. SteveRogers)") },
                singleLine = true,
                enabled = !state.protocolRunning,
                modifier = Modifier.fillMaxWidth()
            )


            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onEvent(TastingEvent.StartProtocol) },
                enabled = state.subject.matches(Regex("[A-Z][a-zA-Z0-9]+")) && !state.protocolRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Protocol")
            }

            Spacer(Modifier.height(24.dp))
            Text(state.stageDescription, fontSize = 18.sp)

            if (state.isRecording) {
                Text("Recording EEG...", color = MaterialTheme.colorScheme.error)
            }

            if (state.showRatingInput) {
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = state.rating,
                    onValueChange = { onEvent(TastingEvent.RatingChanged(it)) },
                    label = { Text("Rate experience (1-5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onEvent(TastingEvent.SubmitRating) },
                    enabled = state.rating.toIntOrNull() in 1..5,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Rating")
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onEvent(TastingEvent.DeleteCsv) },
                modifier = Modifier.fillMaxWidth()

            ) {
                Text("Delete CSV File")
            }

        }
    }

    // Show logout confirmation dialog
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(TastingEvent.DismissLogoutDialog) },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = { onEvent(TastingEvent.ConfirmLogout) }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TastingEvent.DismissLogoutDialog) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            },
        )
    }
}


@Composable
@FoodbackPreview
private fun TastingPreview() {
    FoodbackTheme  {
        Surface {
            TastingScreen()
        }
    }
}