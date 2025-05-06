package it.unipi.msss.wear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import it.unipi.msss.wear.viewmodel.HeartRateViewModel

@Composable
fun HeartRateScreen(viewModel: HeartRateViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            if (state.isCollecting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Raccolta in corso...",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            } else {
                Button(
                    onClick = { viewModel.startCollection() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Avvia Raccolta")
                }

                Spacer(modifier = Modifier.height(16.dp))

                state.avg?.let { avg ->
                    Text(
                        text = "Media: ${String.format("%.1f", avg)} BPM",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                }

                state.stdev?.let { std ->
                    Text(
                        text = "Deviazione Std: ${String.format("%.1f", std)}",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }

                state.error?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
