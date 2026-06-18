package com.stockgro.mediapod.coil.mappers

import coil3.PlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.crossfade
import coil3.size.Size
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.ImageSource
import com.stockgro.mediapod.Transformation
import com.stockgro.mediapod.coil.toPlatformImage
import com.stockgro.mediapod.utils.RequestSize
import coil3.request.ImageRequest as CoilImageRequest


fun ImageRequest.toCoilRequest(context: PlatformContext): CoilImageRequest {
    return toCoilRequestInternal(context, this)
}

private fun toCoilRequestInternal(context: PlatformContext, request: ImageRequest): CoilImageRequest {
    return CoilImageRequest.Builder(context)
        .data(request.data)
        .apply {
            when (val s = request.size) {
                is RequestSize.Original -> size(Size.ORIGINAL)
                is RequestSize.Fixed -> size(s.width, s.height)
            }
        }
        .apply {
            request.target?.let { t ->
                this.target(
                    onStart = { p -> t.onStart(p?.toPlatformImage(context)) },
                    onError = { e -> t.onError(e?.toPlatformImage(context)) },
                    onSuccess = { r -> t.onSuccess(r.toPlatformImage(context)) }
                )
            }
        }
        .applyPlaceholders(request.placeholder, request.error, request.fallback)
        .applyTransformations(request.transformations)
        .memoryCachePolicy(request.memoryCachePolicy.toCoilPolicy())
        .diskCachePolicy(request.diskCachePolicy.toCoilPolicy())
        .apply {
            if (request.headers.isNotEmpty()) {
                httpHeaders(NetworkHeaders.Builder().apply {
                    request.headers.forEach { (k, v) -> add(k, v) }
                }.build())
            }
        }
        .crossfade(if (request.crossfade) request.crossfadeDurationMs else 0)
        .build()
}

internal fun Any.toCoilModel(): Any = when (this) {
    is ImageSource.Url -> url
    is ImageSource.Resource -> resId
    is ImageSource.LocalFile -> path
    is ImageSource.Bytes -> data
    else -> this
}

expect fun CoilImageRequest.Builder.applyPlaceholders(
    placeholder: Any?,
    error: Any?,
    fallback: Any?
): CoilImageRequest.Builder

expect fun CoilImageRequest.Builder.applyTransformations(
    transformations: List<Transformation>
): CoilImageRequest.Builder
