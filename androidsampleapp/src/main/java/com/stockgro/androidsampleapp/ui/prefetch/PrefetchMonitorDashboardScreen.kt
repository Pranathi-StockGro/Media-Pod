package com.stockgro.androidsampleapp.ui.prefetch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrefetchMonitorDashboardScreen(
    viewModel: PrefetchTestViewModel,
    modifier: Modifier = Modifier,
    onVideoSelected: (String) -> Unit
) {
    val videoStates by viewModel.videoStates.collectAsState()
    val isDelayEnabled by viewModel.isDelayEnabled.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Media Prefetch Test",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isDelayEnabled,
                    onCheckedChange = { viewModel.toggleDelay(it) }
                )
                Text("Simulate Network Delay", style = MaterialTheme.typography.bodyMedium)
            }
            
            TextButton(onClick = { viewModel.clearAllCaches() }) {
                Text("Clear Cache", color = MaterialTheme.colorScheme.error)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(videoStates) { state ->
                VideoDiagnosticItem(
                    state = state,
                    onPrefetch = { viewModel.prefetchVideo(state.url) },
                    onPlay = { onVideoSelected(viewModel.getProxyUrl(state.url)) }
                )
            }
        }
    }
}

@Composable
fun VideoDiagnosticItem(
    state: VideoDiagnosticState,
    onPrefetch: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = state.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Status: ${state.prefetchStatus} • ${state.totalSize / (1024 * 1024)} MB",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPrefetch,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Prefetch", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Play", fontSize = 12.sp)
                    }
                }
            }

            if (state.chunks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cache Segments:", fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
                ChunkVisualizer(chunks = state.chunks)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChunkVisualizer(chunks: List<ChunkVisualState>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        chunks.forEach { chunk ->
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 14.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        when (chunk.status) {
                            ChunkStatus.DOWNLOADED -> Color(0xFF4CAF50) // Green
                            ChunkStatus.IN_FLIGHT -> Color(0xFFFFEB3B) // Yellow
                            ChunkStatus.MISSING -> Color(0xFFE0E0E0)   // Light Gray
                        }
                    )
            )
        }
    }
}
