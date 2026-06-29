package com.stockgro.prefetch

/**
 * Represents the current state of a prefetch operation for a specific URL.
 */
sealed interface PrefetchStatus {
    /** No prefetch operation has been initiated. */
    object Idle : PrefetchStatus
    
    /**
     * Prefetching is currently in progress.
     * @property url The URL being prefetched.
     * @property progress The current progress from 0.0 to 1.0.
     */
    data class Loading(
        val url: String,
        val progress: Float
    ) : PrefetchStatus

    /**
     * Prefetching has completed successfully.
     * @property url The prefetched URL.
     * @property filePath Local path to the cached media (if applicable).
     * @property type The type of media prefetched.
     */
    data class Success(
        val url: String,
        val filePath: String,
        val type: PrefetchMediaType
    ) : PrefetchStatus

    /**
     * Prefetching failed.
     * @property url The URL that failed to prefetch.
     * @property exception The error that occurred.
     * @property fallback Optional fallback media to use.
     */
    data class Error(
        val url: String,
        val exception: Throwable,
        val fallback: FallbackMedia? = null
    ) : PrefetchStatus
}