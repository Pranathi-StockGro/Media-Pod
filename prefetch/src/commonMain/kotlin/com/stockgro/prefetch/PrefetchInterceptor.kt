package com.stockgro.prefetch

interface PrefetchInterceptor {
    fun onFailure(url: String, type: PrefetchMediaType, throwable: Throwable) {}
    fun onChunkStart(url: String, index: Int) {}
    fun onChunkSuccess(url: String, index: Int) {}
    fun onChunkFailure(url: String, index: Int, throwable: Throwable) {}
}
