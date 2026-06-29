package com.stockgro.prefetch

/**
 * Defines how much of a media file should be prefetched.
 */
sealed class PrefetchStrategy {
    /**
     * Prefetch only the first [n] chunks of the media file.
     * Useful for enabling instant playback without caching the entire file.
     */
    data class FirstNChunks(val n: Int) : PrefetchStrategy()
    
    /** Prefetch the entire media file. */
    object Full : PrefetchStrategy()
}