package com.stockgro.mediapod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stockgro.mediapod.viewmodel.VideoIntent
import com.stockgro.mediapod.viewmodel.VideoViewModel
import com.stockgro.prefetch.MediaPrefetchManager

@Composable
fun VideoApp(viewModel: VideoViewModel) {
    val state by viewModel.uiState.collectAsState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MediaPod Video Prefetch",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (state.selectedUrl != null) {
                    VideoPlayer(
                        url = state.selectedUrl!!,
                        prefetchManager = viewModel.prefetchManager,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Select a video to play", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text("Select Video", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            state.videos.forEach { (url, title) ->
                val status = state.prefetchStatuses[url]
                Button(
                    onClick = { viewModel.onIntent(VideoIntent.SelectVideo(url)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = if (state.selectedUrl == url) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title)
                        when (status) {
                            is com.stockgro.prefetch.PrefetchStatus.Loading -> {
                                CircularProgressIndicator(
                                    progress = { status.progress },
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }

                            is com.stockgro.prefetch.PrefetchStatus.Success -> {
                                Text("✓", color = Color.Green)
                            }

                            is com.stockgro.prefetch.PrefetchStatus.Error -> {
                                Text("!", color = Color.Red)
                            }

                            else -> {}
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.selectedUrl != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.onIntent(VideoIntent.PrefetchVideo(state.selectedUrl!!))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Prefetch Selected Video")
                }
            }
        }
    }
}

@Composable
expect fun VideoPlayer(
    url: String,
    prefetchManager: MediaPrefetchManager,
    modifier: Modifier = Modifier,
    allowNetworkFallback: Boolean = true
)
