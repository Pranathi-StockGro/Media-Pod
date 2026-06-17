package com.stockgro.mediapod.view

import android.widget.ImageView
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.ImageRequestDisposable

/**
 * Android-only builder extension function.
 */

inline fun ImageView.load(
    data: Any?,
    imageLoader: ImageLoader = ImageLoaderProvider.default, // Auto-fetched via singleton
    builder: ImageRequest.Builder.() -> Unit = {},
): ImageRequestDisposable {
    val request = ImageRequest.Builder(data ?: "")
        .imageTarget(ImageViewTarget(this))
        .apply(builder)
        .build()

    return imageLoader.enqueue(request)
}