package it.unipi.msss.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import it.unipi.msss.wear.viewmodel.HeartRateViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import it.unipi.msss.R

//import androidx.compose.ui.platform.LocalContext
@Composable
fun HeartRateScreen(viewModel: HeartRateViewModel = viewModel()) {
    //val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val hrComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.hr_animation))
    val edaComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.eda_animation))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Spaziatura tra gli elementi
        ) {
             // EDA Animation
            LottieAnimation(
                composition = edaComposition,
                modifier = Modifier.size(50.dp),
                iterations = LottieConstants.IterateForever
            )

            // EDA value
            state.latestEda?.let { eda->
                Text(
                    text = "${eda.toInt()} µS",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )
            } ?: Text(
                text = "No Data",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center
            )

            // HeartRate Animation
            LottieAnimation(
                composition = hrComposition,
                modifier = Modifier.size(50.dp),
                iterations = LottieConstants.IterateForever
            )

            // HeartRate value
            state.latestHeartRate?.let { bpm ->
                Text(
                    text = "${bpm.toInt()} BPM",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )
            } ?: Text(
                text = "No data",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center
            )

        }
    }
}
