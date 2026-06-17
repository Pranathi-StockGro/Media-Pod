package com.stockgro.mediapod

import androidx.compose.runtime.Stable

/**
 * Library-level configuration applied once via [ImageLoader.configure].
 *
 * Usage:
 * ```kotlin
 * val config = ImageLoaderConfig.Builder()
 *     .memoryCache {
 *         maxSizePercent(0.20)
 *         enabled(true)
 *     }
 *     .diskCache {
 *         maxSizeBytes(50L * 1024 * 1024) // 50 MB
 *         directory("/custom/cache/path")  // optional
 *     }
 *     .network {
 *         connectTimeoutMillis(10_000)
 *         readTimeoutMillis(30_000)
 *     }
 *     .logger(MyImageLogger())
 *     .build()
 * ```
 */

class ImageLoaderConfig private constructor(
    val memoryCache: MemoryCacheConfig,
    val diskCache: DiskCacheConfig,
    val networkConfig: NetworkConfig,
    val respectCacheHeaders: Boolean,
) {
    class Builder {
        private var memoryCache = MemoryCacheConfig.Builder().build()
        private var diskCache = DiskCacheConfig.Builder().build()
        private var networkConfig = NetworkConfig.Builder().build()
        private var respectCacheHeaders: Boolean = true

        fun memoryCache(block: MemoryCacheConfig.Builder.() -> Unit) = apply {
            memoryCache = MemoryCacheConfig.Builder().apply(block).build()
        }

        fun diskCache(block: DiskCacheConfig.Builder.() -> Unit) = apply {
            diskCache = DiskCacheConfig.Builder().apply(block).build()
        }

        fun network(block: NetworkConfig.Builder.() -> Unit) = apply {
            networkConfig = NetworkConfig.Builder().apply(block).build()
        }

        fun respectCacheHeaders(respect: Boolean) = apply { respectCacheHeaders = respect }

        fun build() = ImageLoaderConfig(
            memoryCache = memoryCache,
            diskCache = diskCache,
            networkConfig = networkConfig,
//            logger = logger,
            respectCacheHeaders = respectCacheHeaders,
//            placeholderColor = placeholderColor,
        )
    }

    companion object {
        /** A sensible default configuration. */
        fun default() = Builder().build()
    }
}

// ── MemoryCacheConfig ─────────────────────────────────────────────────────────

data class MemoryCacheConfig(
    val enabled: Boolean,
    /**
     * Maximum size as a fraction of available heap (0.0 – 1.0).
     * e.g. 0.25 = 25 % of available memory.
     * Ignored when [maxSizeBytes] > 0.
     */
    val maxSizePercent: Double,
    /**
     * Hard cap in bytes. Takes precedence over [maxSizePercent] when set.
     * 0 means "use [maxSizePercent] instead".
     */
    val maxSizeBytes: Long,
) {
    class Builder {
        private var enabled = true
        private var maxSizePercent = 0.25
        private var maxSizeBytes = 0L

        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        fun maxSizePercent(percent: Double) = apply { this.maxSizePercent = percent.coerceIn(0.0, 1.0) }
        fun maxSizeBytes(bytes: Long) = apply { this.maxSizeBytes = bytes }

        fun build() = MemoryCacheConfig(enabled, maxSizePercent, maxSizeBytes)
    }
}

// ── DiskCacheConfig ───────────────────────────────────────────────────────────

data class DiskCacheConfig(
    val enabled: Boolean,
    /** Maximum on-disk cache size in bytes. Default: 100 MB. */
    val maxSizeBytes: Long,
    /**
     * Absolute path to the cache directory.
     * Null = platform default (Coil: cacheDir/image_cache, Glide: cacheDir/image_manager_disk_cache).
     */
    val directory: String?,
) {
    class Builder {
        private var enabled = true
        private var maxSizeBytes = 100L * 1024 * 1024 // 100 MB
        private var directory: String? = null

        fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
        fun maxSizeBytes(bytes: Long) = apply { this.maxSizeBytes = bytes }
        fun directory(path: String) = apply { this.directory = path }

        fun build() = DiskCacheConfig(enabled, maxSizeBytes, directory)
    }
}

// ── NetworkConfig ─────────────────────────────────────────────────────────────

data class NetworkConfig(
    val connectTimeoutMillis: Long,
    val readTimeoutMillis: Long,
    val writeTimeoutMillis: Long,
) {
    class Builder {
        private var connectTimeoutMillis = 15_000L
        private var readTimeoutMillis = 30_000L
        private var writeTimeoutMillis = 30_000L

        fun connectTimeoutMillis(ms: Long) = apply { connectTimeoutMillis = ms }
        fun readTimeoutMillis(ms: Long) = apply { readTimeoutMillis = ms }
        fun writeTimeoutMillis(ms: Long) = apply { writeTimeoutMillis = ms }

        fun build() = NetworkConfig(connectTimeoutMillis, readTimeoutMillis, writeTimeoutMillis)
    }
}