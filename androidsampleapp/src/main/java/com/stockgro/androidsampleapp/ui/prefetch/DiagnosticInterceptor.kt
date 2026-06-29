package com.stockgro.androidsampleapp.ui.prefetch

import com.stockgro.prefetch.PrefetchInterceptor
import java.util.concurrent.ConcurrentHashMap

class DiagnosticInterceptor : PrefetchInterceptor {
    val inFlightChunks = ConcurrentHashMap.newKeySet<Pair<String, Int>>()

    override fun onChunkStart(url: String, index: Int) {
        inFlightChunks.add(url to index)
    }

    override fun onChunkSuccess(url: String, index: Int) {
        inFlightChunks.remove(url to index)
    }

    override fun onChunkFailure(url: String, index: Int, throwable: Throwable) {
        inFlightChunks.remove(url to index)
    }
}
