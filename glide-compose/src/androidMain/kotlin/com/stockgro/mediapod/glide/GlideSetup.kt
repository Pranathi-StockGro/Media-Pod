package com.stockgro.mediapod.glide

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageLoaderProvider


@Composable
@ReadOnlyComposable
fun InitializeGlideEnginePlatformLoader(
    context: Context,
    config: ImageLoaderConfig
) {

    ImageLoaderProvider.setFactory {
        GlideImageLoaderConfig.applyToGlide(context, config)
        GlideImageLoaderImpl(context)
    }
}