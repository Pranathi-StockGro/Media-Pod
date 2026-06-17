package com.stockgro.mediapod.coil.mappers

import com.stockgro.mediapod.enums.CachePolicy
import coil3.request.CachePolicy as CoilCachePolicy

internal fun CachePolicy.toCoilPolicy(): CoilCachePolicy {
    return when (this) {
        CachePolicy.ENABLED -> CoilCachePolicy.ENABLED
        CachePolicy.DISABLED -> CoilCachePolicy.DISABLED
        CachePolicy.READ_ONLY -> CoilCachePolicy.READ_ONLY
        CachePolicy.WRITE_ONLY -> CoilCachePolicy.WRITE_ONLY
    }
}