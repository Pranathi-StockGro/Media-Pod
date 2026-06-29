package com.stockgro.prefetch

sealed class PrefetchStrategy {
    data class FirstNChunks(val n: Int) : PrefetchStrategy()
    object Full : PrefetchStrategy()
}