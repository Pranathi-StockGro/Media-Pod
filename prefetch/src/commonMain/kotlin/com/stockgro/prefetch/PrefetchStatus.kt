package com.stockgro.prefetch

sealed interface PrefetchStatus {
    object Idle : PrefetchStatus
    
    data class Loading(
        val url: String,
        val progress: Float // 0.0 to 1.0
    ) : PrefetchStatus

    data class Success(
        val url: String,
        val filePath: String,
        val type: PrefetchMediaType
    ) : PrefetchStatus

    data class Error(
        val url: String,
        val exception: Throwable,
        val fallback: FallbackMedia? = null
    ) : PrefetchStatus
}