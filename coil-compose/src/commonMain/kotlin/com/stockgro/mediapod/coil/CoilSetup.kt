package com.stockgro.mediapod.coil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import coil3.compose.LocalPlatformContext
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


@Composable
@ReadOnlyComposable
fun SetSingletonCoilImageLoaderFactory(config: ImageLoaderConfig) {
    val context = LocalPlatformContext.current
    ImageLoaderProvider.setFactory {
        val loader = CoilImageLoaderImpl(context)   // pass Android Context
        loader.configure(config)
        loader
    }
}