package com.stockgro.mediapod.coil.mappers

import coil3.Image
import coil3.request.ImageRequest

actual fun ImageRequest.Builder.applyPlaceholders(
    placeholder: Any?,
    error: Any?,
    fallback: Any?
): ImageRequest.Builder = apply {
    (placeholder?.toCoilModel() as? Image)?.let { this.placeholder(it) }
    (error?.toCoilModel() as? Image)?.let { this.error(it) }
    (fallback?.toCoilModel() as? Image)?.let { this.fallback(it) }
}