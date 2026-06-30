package com.stockgro.mediapod.glide

import android.content.Context
import androidx.compose.runtime.Composable
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


@Composable
fun SetGlideImageLoaderFactory(
    context: Context,
    config: ImageLoaderConfig
) {
    if (ImageLoaderProvider.isInitialized) return

    ImageLoaderProvider.setFactory {
        val loader = GlideImageLoaderImpl(context)
        loader.configure(config)
        loader
    }
}