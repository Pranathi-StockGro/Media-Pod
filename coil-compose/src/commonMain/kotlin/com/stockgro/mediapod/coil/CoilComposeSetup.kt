package com.stockgro.mediapod.coil

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


@Composable
fun setupCoilImageLoader(config: ImageLoaderConfig) {
    if (ImageLoaderProvider.isInitialized) return

    val context = LocalPlatformContext.current
    ImageLoaderProvider.setFactory {
        val loader = CoilImageLoaderImpl(context)   // pass Android Context
        loader.configure(config)
        loader
    }
}