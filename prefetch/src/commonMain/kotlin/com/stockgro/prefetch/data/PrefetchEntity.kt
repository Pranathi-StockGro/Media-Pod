package com.stockgro.prefetch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prefetch_metadata")
data class PrefetchEntity(
    @PrimaryKey val url: String,
    val totalSize: Long,
    val etag: String?,
    val contentType: String?,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val chunkSize: Int
)