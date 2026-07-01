package com.stockgro.mediapod.glide

import androidx.compose.runtime.Composable
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider
import androidx.compose.ui.platform.LocalContext

@Composable
fun setupGlideImageLoader(
    config: ImageLoaderConfig
) {
    if (ImageLoaderProvider.isInitialized) return

    val context = LocalContext.current
    ImageLoaderProvider.setFactory {
        val loader = GlideImageLoaderImpl(context)
        loader.configure(config)
        loader
    }
}