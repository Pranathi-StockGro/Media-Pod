package com.stockgro.mediapod.glide

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


@Composable
@ReadOnlyComposable
fun InitializeGlideForCompose(
    context: Context,
    config: ImageLoaderConfig
) {

    ImageLoaderProvider.setFactory {
        val loader = GlideImageLoaderImpl(context)
        loader.configure(config)
        loader
    }
}