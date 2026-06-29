package com.stockgro.prefetch

import com.stockgro.prefetch.exception.InvalidConfigException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class PrefetchConfig(
    val maxFileAge: Duration = 24.hours,
    val bufferSize: Int = 8192,
    val chunkSize: Int = 2 * 1024 * 1024, // 2MB default
    val retryDelay: Duration = 1.seconds,
    val maxRetries: Int = 3
) {
    init {
        if (bufferSize <= 0) throw InvalidConfigException("bufferSize must be > 0")
        if (chunkSize <= 0) throw InvalidConfigException("chunkSize must be > 0")
        if (maxFileAge.isNegative()) throw InvalidConfigException("maxFileAge cannot be negative")
        if (maxRetries < 0) throw InvalidConfigException("maxRetries cannot be negative")
    }
}
