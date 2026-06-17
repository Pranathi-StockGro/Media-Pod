package com.stockgro.mediapod

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.Painter

actual class PlatformImage actual constructor(
    actual val painter: Painter
) {
    // A standard property that initializes as null by default
    var nativeDrawable: Drawable? = null
        private set

    // The secondary constructor your engines will call to attach the Drawable
    constructor(painter: Painter, nativeDrawable: Drawable?) : this(painter) {
        this.nativeDrawable = nativeDrawable
    }
}