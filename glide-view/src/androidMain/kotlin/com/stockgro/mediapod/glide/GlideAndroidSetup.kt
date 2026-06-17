package com.stockgro.mediapod.glide

import android.content.Context
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


fun Context.initializeGlideImageLoader(config: ImageLoaderConfig) {
    ImageLoaderProvider.setFactory {
        val loader = GlideImageLoaderImpl(this)
        loader.configure(config)
        loader
    }
}