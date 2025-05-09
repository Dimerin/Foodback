package it.unipi.msss.foodback.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.unipi.msss.foodback.viewmodel.HeartRatePhoneViewModel
@Composable
fun HeartRatePhoneScreen(viewModel: HeartRatePhoneViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Ultimi dati ricevuti",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.lastAvg != null && state.lastStdev != null) {
                Text(
                    text = "Media HR: ${String.format("%.1f", state.lastAvg)} BPM",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Deviazione HR: ${String.format("%.1f", state.lastStdev)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.edaAvg != null && state.edaStdev != null) {
                Text(
                    text = "Media EDA: ${String.format("%.2f", state.edaAvg)} µS",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Deviazione EDA: ${String.format("%.2f", state.edaStdev)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (state.lastAvg == null && state.edaAvg == null) {
                Text(
                    text = "In attesa di dati...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}