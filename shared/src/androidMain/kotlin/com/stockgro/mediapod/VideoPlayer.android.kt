package com.stockgro.mediapod

import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.datasource.ChunkMergerDataSourceFactory
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    prefetchManager: MediaPrefetchManager,
    modifier: Modifier,
    allowNetworkFallback: Boolean
) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    val dataSourceFactory = remember(url, allowNetworkFallback) {
        val chunkMerger = try {
            runBlocking { prefetchManager.getChunkMerger(url) }
        } catch (e: Exception) {
            null
        }

        if (chunkMerger != null) {
            ChunkMergerDataSourceFactory { chunkMerger }
        } else if (allowNetworkFallback) {
            androidx.media3.datasource.DefaultDataSource.Factory(context)
        } else {
            ChunkMergerDataSourceFactory { throw IllegalStateException("Prefetch not available and network fallback disabled") }
        }
    }

    LaunchedEffect(url) {
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}
