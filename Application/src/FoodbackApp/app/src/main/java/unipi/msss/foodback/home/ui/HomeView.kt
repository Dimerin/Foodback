package unipi.msss.foodback.home.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    viewModel: HomeViewModel = hiltViewModel(),
    onLoggedOut: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Home(state = state, onEvent = { viewModel.onEvent(it) })
    ViewEvent(viewModel.eventsFlow) { event ->
        when (event) {
            HomeNavigationEvents.LoggedOut -> {
                onLoggedOut()
                Toast.makeText(context, "Logged out", Toast.LENGTH_LONG).show()
            }
            is HomeNavigationEvents.Error -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    state: HomeState = HomeState(),
    onEvent: (HomeEvent) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluation") },
                actions = {
                    IconButton(onClick = { onEvent.invoke(HomeEvent.ShowLogoutDialog) }) {
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

            ElevatedCard(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                modifier = Modifier.size(width = 500.dp, height = 350.dp).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LottieAnimation(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        composition = rememberLottieComposition(
                            spec = LottieCompositionSpec.RawRes(
                                when (state.stage) {
                                    HomeStage.Idle -> R.raw.home_anim
                                    HomeStage.InPreparation -> R.raw.mouth_anim
                                    HomeStage.InProgress -> R.raw.inference_anim
                                    HomeStage.Finished -> R.raw.end_anim
                                }
                            )
                        ).value,
                        iterations = if (state.isFinished) 1 else LottieConstants.IterateForever
                    )

                    Spacer(Modifier.height(16.dp))

                    val name = state.name ?: "User"

                if(state.isIdle || state.isFinished) {
                    Text(
                        text = "Welcome, $name!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                    Text(
                        text = state.stageDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onEvent(HomeEvent.StartEvaluation) },
                enabled = state.isEEGConnected
                        && state.isWatchConnected
                        && (state.isFinished
                        || state.stage == HomeStage.Idle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Evaluate")
            }
            Spacer(Modifier.height(36.dp))
            ElevatedCard( elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
                modifier = Modifier.size(width = 250.dp, height = 150.dp)
            ){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Devices Status",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )


                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    )
                    {
                        Icon(
                            painter = painterResource(id = if (state.isEEGConnected) R.drawable.eeg_connected else R.drawable.eeg_disconnected),
                            contentDescription = "EEG Device Status",
                            tint = if (state.isEEGConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Icon(
                            painter = painterResource(id = if (state.isWatchConnected) R.drawable.watch_connected else R.drawable.watch_disconnected),
                            contentDescription = "EEG Device Status",
                            tint = if (state.isWatchConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            if (state.isFinished) {
                Spacer(Modifier.height(16.dp))
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Predicted Rating",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0..4) {
                            Icon(
                                imageVector = if (i <= state.predictedRating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Star $i",
                                tint = if (i <= state.predictedRating) Color.Yellow else Color.Gray,
                                modifier = Modifier
                                    .size(40.dp)

                            )
                        }
                    }
                }
            }
        }
    }

    // Show logout confirmation dialog
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(HomeEvent.DismissLogoutDialog) },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = { onEvent(HomeEvent.ConfirmLogout) }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(HomeEvent.DismissLogoutDialog) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            },
        )
    }

}

@Composable
@FoodbackPreview
private fun HomePreview() {
    FoodbackTheme  {
        Surface {
            Home()
        }
    }
}