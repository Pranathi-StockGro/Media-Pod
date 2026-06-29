package com.stockgro.mediapod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stockgro.prefetch.MediaPrefetchManager

@Composable
fun VideoApp(prefetchManager: MediaPrefetchManager) {
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    
    val videos = listOf(
        "https://vjs.zencdn.net/v/oceans.mp4" to "Oceans (Small)",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4" to "Exo Screens (Medium)",
        "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_10MB.mp4" to "Bunny 1080p (Large)",
        "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_20MB.mp4" to "Bunny 1080p (High Bitrate)"
    )
    
    val statusMap by prefetchManager.statusMap.collectAsState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MediaPod Video Prefetch", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (selectedUrl != null) {
                    VideoPlayer(
                        url = selectedUrl!!,
                        prefetchManager = prefetchManager,
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

            videos.forEach { (url, title) ->
                val status = statusMap[url]
                Button(
                    onClick = { selectedUrl = url },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = if (selectedUrl == url) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
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
            
            if (selectedUrl != null) {
                OutlinedButton(
                    onClick = { 
                        prefetchManager.prefetchVideos(
                            listOf(selectedUrl!!), 
                            com.stockgro.prefetch.PrefetchMediaType.MP4, 
                            com.stockgro.prefetch.PrefetchStrategy.Full
                        )
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
