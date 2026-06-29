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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
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

    // Resolve the data source factory asynchronously to avoid ANR
    val dataSourceFactoryState = produceState<androidx.media3.datasource.DataSource.Factory?>(
        initialValue = null, 
        url, 
        allowNetworkFallback
    ) {
        val chunkMerger = try {
            prefetchManager.getChunkMerger(url)
        } catch (e: Exception) {
            null
        }

        value = if (chunkMerger != null) {
            val upstreamFactory = if (allowNetworkFallback) {
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(30000)
                    .setReadTimeoutMs(30000)
                    .setAllowCrossProtocolRedirects(true)
                DefaultDataSource.Factory(context, httpFactory)
            } else null
            ChunkMergerDataSourceFactory(upstreamFactory) { chunkMerger }
        } else if (allowNetworkFallback) {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)
            DefaultDataSource.Factory(context, httpFactory)
        } else {
            null
        }
    }

    val dataSourceFactory = dataSourceFactoryState.value

    LaunchedEffect(dataSourceFactory) {
        if (dataSourceFactory != null) {
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
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
        modifier = modifier,
        update = {
            it.player = exoPlayer
        }
    )
}
