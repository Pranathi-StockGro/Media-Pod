package com.stockgro.mediapod.coil


import coil3.ImageLoader
import com.stockgro.mediapod.NetworkConfig

expect object CoilPlatformConfig {

    fun applyNetworkFetcher(
        builder: ImageLoader.Builder,
        config: NetworkConfig,
        respectCacheHeaders: Boolean
    )
}