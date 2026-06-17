package com.stockgro.mediapod.coil

import coil3.Image
import coil3.PlatformContext
import coil3.asDrawable
import coil3.compose.asPainter
import com.stockgro.mediapod.PlatformImage

internal actual fun Image.toPlatformImage(context: PlatformContext): PlatformImage {
    return platformImageFrom(this, context)
}

internal actual fun platformImageFrom(image: Image, context: PlatformContext): PlatformImage {
    val platformImage = PlatformImage(
        image.asPainter(context),
        image.asDrawable(context.resources)
    )

    return platformImage
}