package com.stockgro.prefetch

import com.stockgro.prefetch.exception.InvalidConfigException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the Media Prefetcher.
 *
 * @property maxFileAge Maximum age of a cached chunk before it's considered stale and pruned.
 * @property maxCacheSize Maximum total size of the cache in bytes. (Note: Size-based eviction is a planned optimization).
 * @property bufferSize Size of the buffer used for disk I/O operations.
 * @property chunkSize Size of each prefetchable unit (chunk) in bytes.
 * @property retryDelay Initial delay before retrying a failed chunk download.
 * @property maxRetries Maximum number of attempts to download a single chunk.
 */
data class PrefetchConfig(
    val maxFileAge: Duration = 24.hours,
    val maxCacheSize: Long = 500 * 1024 * 1024, // 500MB default
    val bufferSize: Int = 8192,
    val chunkSize: Int = 2 * 1024 * 1024, // 2MB default
    val retryDelay: Duration = 1.seconds,
    val maxRetries: Int = 3
) {
    init {
        if (bufferSize <= 0) throw InvalidConfigException("bufferSize must be > 0")
        if (chunkSize <= 0) throw InvalidConfigException("chunkSize must be > 0")
        if (maxCacheSize <= 0) throw InvalidConfigException("maxCacheSize must be > 0")
        if (maxFileAge.isNegative()) throw InvalidConfigException("maxFileAge cannot be negative")
        if (maxRetries < 0) throw InvalidConfigException("maxRetries cannot be negative")
    }
}
