package com.stockgro.prefetch

sealed interface PrefetchStatus {
    object Loading : PrefetchStatus

    data class Success(
        val filePath: String,
        val type: PrefetchMediaType
    ) : PrefetchStatus

    data class Error(
        val exception: Throwable,
        val fallback: FallbackMedia
    ) : PrefetchStatus
}