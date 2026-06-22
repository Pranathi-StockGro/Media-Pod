package com.stockgro.prefetch

interface PrefetchInterceptor {
    fun onStart(url: String, type: PrefetchMediaType)
    fun onSuccess(url: String, type: PrefetchMediaType, sizeBytes: Long)
    fun onFailure(url: String, type: PrefetchMediaType, throwable: Throwable)
}