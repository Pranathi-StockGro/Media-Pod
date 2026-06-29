package com.stockgro.mediapod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.datasource.MediaPodResourceLoaderDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.resourceLoader
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    prefetchManager: MediaPrefetchManager,
    modifier: Modifier,
    allowNetworkFallback: Boolean
) {
    val scope = rememberCoroutineScope()
    val playerViewController = remember { AVPlayerViewController() }

    val playerState = produceState<Pair<AVPlayer, Any>?>(initialValue = null, url, allowNetworkFallback) {
        val chunkMerger = try {
            prefetchManager.getChunkMerger(url)
        } catch (e: Exception) {
            null
        }

        val result = if (chunkMerger != null) {
            val originalUrl = NSURL.URLWithString(url)!!
            val components = NSURLComponents.componentsWithURL(originalUrl, resolvingAgainstBaseURL = false)!!
            components.scheme = "mediapod-prefetch"
            val customUrl = components.URL!!

            val asset = AVURLAsset.assetWithURL(customUrl)
            val delegate = MediaPodResourceLoaderDelegate(chunkMerger, scope)
            asset.resourceLoader.setDelegate(delegate, dispatch_get_main_queue())

            val playerItem = AVPlayerItem.playerItemWithAsset(asset)
            Pair(AVPlayer.playerWithPlayerItem(playerItem), delegate)
        } else if (allowNetworkFallback) {
            Pair(AVPlayer.playerWithURL(NSURL.URLWithString(url)!!), Unit)
        } else {
            null
        }

        value = result

        if (result != null) {
            val player = result.first
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                player.pause()
                player.replaceCurrentItemWithPlayerItem(null)
            }
        }
    }

    val player = playerState.value?.first

    LaunchedEffect(player) {
        playerViewController.player = player
        player?.play()
    }

    UIKitView(
        factory = {
        playerViewController.view
    },
        modifier = modifier,
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
    )
}
