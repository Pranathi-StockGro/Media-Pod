package com.stockgro.mediapod

interface ImageLoader {

    /** Execute a pre-built request and return a result. */
    suspend fun execute(request: ImageRequest): ImageResult

    /** Enqueue a request without waiting (fire-and-forget into target). */
    fun enqueue(request: ImageRequest): ImageRequestDisposable

    /** Apply library-level configuration — called once at init time. */
    fun configure(config: ImageLoaderConfig)

    /** Preload an image into cache without displaying it. */
    suspend fun prefetch(data: Any): ImageResult

    /** Evict all memory cache entries. */
    fun clearMemoryCache()

    /** Evict all disk cache entries. */
    suspend fun clearDiskCache()

    /** Cancel all in-flight requests. */
    fun shutdown()
}