package com.stockgro.mediapod.coil

import coil3.PlatformContext
import platform.UIKit.UIScreen

actual fun getPlatformDpr(context: PlatformContext): Float {
    return UIScreen.mainScreen.scale.toFloat()
}