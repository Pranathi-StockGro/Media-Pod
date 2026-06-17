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
        // Apply global Android parameters
        GlideImageLoaderConfig.applyToGlide(context, config)

        // Return your pure Android-implemented multiplatform executor
        GlideImageLoaderImpl(context)
    }
}