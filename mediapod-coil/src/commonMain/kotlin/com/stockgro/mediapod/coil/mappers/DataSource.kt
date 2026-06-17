package com.stockgro.mediapod.coil.mappers

import coil3.decode.DataSource.*
import com.stockgro.mediapod.enums.DataSource
import coil3.decode.DataSource as CoilDataSource

internal fun CoilDataSource.toDataSource(): DataSource = when (this) {
    MEMORY_CACHE -> DataSource.MEMORY_CACHE
    MEMORY -> DataSource.MEMORY
    DISK -> DataSource.DISK
    NETWORK -> DataSource.NETWORK
}