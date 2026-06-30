package com.stockgro.mediapod.glide

import android.content.Context
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


fun Context.setupGlideImageLoader(config: ImageLoaderConfig) {
    if (ImageLoaderProvider.isInitialized) return

    ImageLoaderProvider.setFactory {
        val loader = GlideImageLoaderImpl(this)
        loader.configure(config)
        loader
    }
}