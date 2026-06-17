package com.stockgro.mediapod.utils

import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline


sealed class RequestSize {
    /** Decode at the image's intrinsic size (no downsampling). */
    object Original : RequestSize()

    /** Decode at exactly [width] × [height] pixels. */
    data class Fixed(val width: Int, val height: Int) : RequestSize()
}

