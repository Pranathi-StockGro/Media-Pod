package com.stockgro.mediapod.coil

import android.content.Context
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider

fun Context.initializeCoilImageLoader(config: ImageLoaderConfig) {
    ImageLoaderProvider.setFactory {
        val loader = CoilImageLoaderImpl(this)
        loader.configure(config)
        loader
    }
}