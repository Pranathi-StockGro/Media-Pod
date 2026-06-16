package com.stockgro.mediapod.coil

import coil3.Image
import coil3.PlatformContext
import coil3.compose.asPainter
import com.stockgro.mediapod.PlatformImage

actual fun Image.toPlatformImage(context: PlatformContext): PlatformImage {
    return platformImageFrom(this, context)
}

actual fun platformImageFrom(image: coil3.Image, context: PlatformContext): PlatformImage {
    return PlatformImage(image.asPainter(context))
}