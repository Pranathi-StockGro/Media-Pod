package com.stockgro.mediapod.coil

import coil3.PlatformContext

actual fun getPlatformDpr(context: PlatformContext): Float {
    return context.applicationContext.resources.displayMetrics.density
}