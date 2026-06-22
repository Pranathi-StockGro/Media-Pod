package com.stockgro.prefetch

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

data class PrefetchConfig(
    val maxFileAge: Duration = 24.hours,
    val bufferSize: Int = 8192,
)