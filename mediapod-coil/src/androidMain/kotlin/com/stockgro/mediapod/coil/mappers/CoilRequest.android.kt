package com.stockgro.mediapod.coil.mappers

import android.graphics.drawable.Drawable
import coil3.request.ImageRequest
import coil3.request.placeholder
import coil3.request.error
import coil3.request.fallback
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation
import com.stockgro.mediapod.Transformation

import coil3.request.transformations

internal actual fun ImageRequest.Builder.applyPlaceholders(
    placeholder: Any?,
    error: Any?,
    fallback: Any?
): ImageRequest.Builder = apply {
    val p = placeholder?.toCoilModel()
    if (p is Int) this.placeholder(p)
    if (p is Drawable) this.placeholder(p)

    val e = error?.toCoilModel()
    if (e is Int) this.error(e)
    if (e is Drawable) this.error(e)

    val f = fallback?.toCoilModel()
    if (f is Int) this.fallback(f)
    if (f is Drawable) this.fallback(f)
}

internal actual fun ImageRequest.Builder.applyTransformations(
    transformations: List<Transformation>
): ImageRequest.Builder = apply {
    if (transformations.isNotEmpty()) {
        this.transformations(transformations.map { transform ->
            when (transform) {
                is Transformation.CircleCrop -> CircleCropTransformation()
                is Transformation.RoundedCorners -> RoundedCornersTransformation(transform.radiusPx)
                is Transformation.Blur -> {
                    // Custom blur if needed, or no-op if handled by Modifiers
                    object : coil3.transform.Transformation() {
                        override val cacheKey: String = "blur_${transform.radius}"
                        override suspend fun transform(input: android.graphics.Bitmap, size: coil3.size.Size): android.graphics.Bitmap = input
                    }
                }
            }
        })
    }
}
