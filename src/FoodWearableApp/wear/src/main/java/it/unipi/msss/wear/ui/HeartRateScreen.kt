package it.unipi.msss.wear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

            Text(
                text = "Heart Rate",
                style = MaterialTheme.typography.title2,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            state.latestHeartRate?.let { bpm ->
                Text(
                    text = "${bpm.toInt()} BPM",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            } ?: Text(
                text = "Nessun dato",
                style = MaterialTheme.typography.body1,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isCollecting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rilevamento in corso...",
                    style = MaterialTheme.typography.body2
                )
            } else {
                Button(onClick = { viewModel.startCollection() }) {
                    Text("Avvia rilevamento")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.saveData() }) {
                Text("Salva dati")
            }

            state.error?.let { errorMsg ->
                Spacer(modifier = Modifier.height(16.dp))
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
