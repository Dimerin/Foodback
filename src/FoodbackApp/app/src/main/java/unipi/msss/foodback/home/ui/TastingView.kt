package unipi.msss.foodback.home.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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


@Composable
fun TastingView(
    viewModel: TastingViewModel = hiltViewModel(),
    onFinished: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current



    TastingScreen(state = state, onEvent = { viewModel.onEvent(it) })

    ViewEvent(viewModel.eventsFlow) { event ->
        when (event) {
            is TastingNavigationEvents.Finished -> onFinished()
            is TastingNavigationEvents.ShareCsvFile -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, event.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(shareIntent)
            }
            is TastingNavigationEvents.Error -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TastingScreen(
    state: TastingState,
    onEvent: (TastingEvent) -> Unit,
) {

    val textColor = MaterialTheme.colorScheme.primary
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasting Protocol") }
            )
        },


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

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.subject,
                onValueChange = { onEvent(TastingEvent.SubjectChanged(it)) },
                label = { Text("Subject (e.g. NicolaSecco)") },
                singleLine = true,
                enabled =  !state.protocolRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onEvent(TastingEvent.StartProtocol) },
                enabled =  state.subject.matches(Regex("[A-Z][a-zA-Z0-9]+")) && !state.protocolRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Protocol")
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = state.stageDescription,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (state.isRecording) {
                Spacer(Modifier.height(24.dp))
                Text("Recording EEG...", color = MaterialTheme.colorScheme.error)
            }

            if (state.showRatingInput) {
                Spacer(Modifier.height(24.dp))

                // Star rating system
                var selectedRating by remember { mutableIntStateOf(state.rating.toIntOrNull() ?: 0) }
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= selectedRating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= selectedRating) Color.Yellow else Color.Gray,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    indication = null, // Disable ripple effect
                                    interactionSource = remember { MutableInteractionSource() } // Prevent interaction tracking
                                ) {
                                    selectedRating = i
                                    onEvent(TastingEvent.RatingChanged(i.toString()))
                                }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onEvent(TastingEvent.SubmitRating) },
                    enabled = selectedRating in 1..5,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Rating")
                }
            }

            if (state.sensorData.isNotEmpty() && state.isRecording) {
                Spacer(Modifier.height(24.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    val maxDataPoints = 50
                    val displayedData = state.sensorData.takeLast(maxDataPoints)
                    val step = size.width / maxDataPoints

                    val channels = listOf(
                        displayedData.map { it.channel1.toFloat() },
                        displayedData.map { it.channel2.toFloat() },
                        displayedData.map { it.channel3.toFloat() },
                        displayedData.map { it.channel4.toFloat() },
                        displayedData.map { it.channel5.toFloat() },
                        displayedData.map { it.channel6.toFloat() }
                    )

                    val maxY = channels.flatten().maxOrNull() ?: 1f
                    val minY = channels.flatten().minOrNull() ?: 0f

                    val colors = listOf(
                        Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta
                    )

                    // Draw Y-axis labels and ticks
                    val yStep = (maxY - minY) / 5
                    for (i in 0..5) {
                        val yValue = minY + i * yStep
                        val yPosition = size.height - ((yValue - minY) / (maxY - minY) * size.height)
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "%.1f".format(yValue),
                                0f,
                                yPosition,
                                android.graphics.Paint().apply {
                                    color = textColor.toArgb()
                                    textSize = 24f
                                }
                            )
                        }
                    }


                    // Draw axis labels
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            "µ V",
                            10f,
                            20f,
                            android.graphics.Paint().apply {
                                color = textColor.toArgb()
                                textSize = 28f
                            }
                        )

                        canvas.nativeCanvas.drawText(
                            "Number of Measures",
                            size.width / 2,
                            size.height + 40f,
                            android.graphics.Paint().apply {
                                color = textColor.toArgb()
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    // Draw the data lines
                    channels.forEachIndexed { index, channelData ->
                        for (i in 1 until channelData.size) {
                            val x1 = (i - 1) * step
                            val y1 = size.height - ((channelData[i - 1] - minY) / (maxY - minY) * size.height)
                            val x2 = i * step
                            val y2 = size.height - ((channelData[i] - minY) / (maxY - minY) * size.height)

                            drawLine(
                                color = colors[index],
                                start = androidx.compose.ui.geometry.Offset(x1, y1),
                                end = androidx.compose.ui.geometry.Offset(x2, y2),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
          if (state.isFinished || state.isIdle ) {
              Text(
                  text = if (state.isDeviceConnected) "Mindrove Connected" else "Mindrove Disconnected",
                  fontSize = 18.sp,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.Center,
                  color = if (state.isDeviceConnected) Color.Green else Color.Red
              )
              Spacer(Modifier.height(24.dp))
          }
          Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "eeg_tasting_data.csv",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onEvent(TastingEvent.DeleteCsv) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete CSV File",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(onClick = { onEvent(TastingEvent.ShareCsv) }) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share CSV File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


