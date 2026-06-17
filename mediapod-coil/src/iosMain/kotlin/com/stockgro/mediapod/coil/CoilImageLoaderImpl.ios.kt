package com.stockgro.mediapod.coil

import coil3.Image
import coil3.PlatformContext
import coil3.compose.asPainter
import com.stockgro.mediapod.PlatformImage

internal actual fun Image.toPlatformImage(context: PlatformContext): PlatformImage {
    return platformImageFrom(this, context)
}

internal actual fun platformImageFrom(image: Image, context: PlatformContext): PlatformImage {
    return PlatformImage(image.asPainter(context))
}
