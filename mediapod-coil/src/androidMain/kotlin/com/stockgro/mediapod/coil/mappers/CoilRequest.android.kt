package com.stockgro.mediapod.coil.mappers

import android.graphics.drawable.Drawable
import coil3.request.ImageRequest
import coil3.request.placeholder
import coil3.request.error
import coil3.request.fallback

actual fun ImageRequest.Builder.applyPlaceholders(
    placeholder: Any?,
    error: Any?,
    fallback: Any?
): ImageRequest.Builder = apply {
    placeholder?.toCoilModel()?.let { model ->
        when (model) {
            is Int -> placeholder(model)
            is Drawable -> placeholder(model)
        }
    }
    error?.toCoilModel()?.let { model ->
        when (model) {
            is Int -> error(model)
            is Drawable -> error(model)
        }
    }
    fallback?.toCoilModel()?.let { model ->
        when (model) {
            is Int -> fallback(model)
            is Drawable -> fallback(model)
        }
    }
}
