package unipi.msss.foodback.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import unipi.msss.foodback.R
import unipi.msss.foodback.viewmodel.WearableViewModel
import kotlin.math.min


//import androidx.compose.ui.platform.LocalContext

@Composable
fun WearableScreen(viewModel: WearableViewModel = viewModel()) {
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
        if (state.isCollecting) {
            RedBorderOverlay()
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Spacing between elements
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
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
            }
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
}

@Composable
fun RedBorderOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp) // Internal margin, to not cut the edge
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height)
            val strokeWidth = 2.dp.toPx()
            drawCircle(
                color = Color.Red,
                radius = diameter / 2 - strokeWidth / 2,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
