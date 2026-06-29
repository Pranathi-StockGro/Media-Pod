package com.stockgro.prefetch

/**
 * Supported media types for prefetching and their associated file extensions.
 */
enum class PrefetchMediaType(val extension: String) {
    /** Adobe After Effects animations. */
    LOTTIE("json"),
    /** Animated images. */
    GIF("gif"),
    /** MPEG-4 video container. */
    MP4("mp4"),
    /** Static images. */
    IMAGE("jpg")
}