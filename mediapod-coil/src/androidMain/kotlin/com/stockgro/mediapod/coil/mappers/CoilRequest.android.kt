package com.stockgro.mediapod.coil.mappers

import android.graphics.Bitmap.Config
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import coil3.Bitmap
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
//                is Transformation.Blur -> {
//                    object : coil3.transform.Transformation() {
//                        override val cacheKey: String = "blur_${transform.radius}"
//
//                        @RequiresApi(Build.VERSION_CODES.O)
//                        override suspend fun transform(
//                            input: Bitmap,
//                            size: coil3.size.Size,
//                        ): Bitmap {
//                            val softInput = if (input.config == Config.HARDWARE) {
//                                input.copy(Config.ARGB_8888, false)
//                            } else {
//                                input
//                            }
//
//                            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                                applyScaleBlur(softInput, transform.radius)
//                            } else {
//                                applyScaleBlur(softInput, transform.radius)
//                            }
//                        }
//
//                        /**
//                         * Scale-based blur that actually respects [radius].
//                         *
//                         * A higher radius → more aggressive downscale → stronger blur.
//                         * Clamped so the bitmap never collapses below 1px.
//                         */
//                        private fun applyScaleBlur(input: Bitmap, radius: Float): Bitmap {
//                            val scaleFactor = (radius / 2f).coerceIn(2f, 16f).toInt()
//
//                            val scaledWidth = (input.width / scaleFactor).coerceAtLeast(1)
//                            val scaledHeight = (input.height / scaleFactor).coerceAtLeast(1)
//
//                            val downscaled = Bitmap.createScaledBitmap(input, scaledWidth, scaledHeight, true)
//                            val blurred = Bitmap.createScaledBitmap(downscaled, input.width, input.height, true)
//
//                            if (downscaled !== blurred && !downscaled.isRecycled) {
//                                downscaled.recycle()
//                            }
//                            return blurred
//                        }
//                    }
//                }
            }
        })
    }
}
