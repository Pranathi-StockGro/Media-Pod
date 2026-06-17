package com.stockgro.mediapod.coil.mappers

import coil3.Image
import coil3.request.ImageRequest

import com.stockgro.mediapod.Transformation

internal actual fun ImageRequest.Builder.applyPlaceholders(
    placeholder: Any?,
    error: Any?,
    fallback: Any?
): ImageRequest.Builder = apply {
    val p = placeholder?.toCoilModel()
    if (p is Image) this.placeholder(p)

    val e = error?.toCoilModel()
    if (e is Image) this.error(e)

    val f = fallback?.toCoilModel()
    if (f is Image) this.fallback(f)
}

internal actual fun ImageRequest.Builder.applyTransformations(
    transformations: List<Transformation>
): ImageRequest.Builder = this
