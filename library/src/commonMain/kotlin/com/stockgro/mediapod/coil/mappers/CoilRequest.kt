package com.stockgro.mediapod.coil.mappers

import coil3.PlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.crossfade
import coil3.size.Size
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.utils.RequestSize
import coil3.request.ImageRequest as CoilImageRequest


fun ImageRequest.toCoilRequest(context: PlatformContext): CoilImageRequest {
    return CoilImageRequest.Builder(context)
        .data(data)
        .apply {
            when (val s = size) {
                is RequestSize.Original -> size(Size.ORIGINAL)
                is RequestSize.Fixed -> size(s.width, s.height)
            }
        }
        // Placeholder / error / fallback
//        .apply { placeholder?.let { placeholder(it.toCoilModel()) } }
//        .apply { error?.let { error(it.toCoilModel()) } }
//        .apply { fallback?.let { fallback(it.toCoilModel()) } }
        // Cache policy
        .memoryCachePolicy(memoryCachePolicy.toCoilPolicy())
        .diskCachePolicy(diskCachePolicy.toCoilPolicy())
        .apply {
            if (headers.isNotEmpty()) {
                httpHeaders(NetworkHeaders.Builder().apply {
                    headers.forEach { (k, v) -> add(k, v) }
                }.build())
            }
        }
        // Transformations
//        .transformations(transformations.map { it.toCoilTransformation() })
        .crossfade(if (crossfade) crossfadeDurationMs else 0)
        .build()
}