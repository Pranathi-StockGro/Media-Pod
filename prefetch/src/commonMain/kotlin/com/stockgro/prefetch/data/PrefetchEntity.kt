package com.stockgro.prefetch.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "media_prefetch_registry")
data class PrefetchEntity(
    @PrimaryKey
    val url: String,
    val type: String,
    val localPath: String,
    val downloadedAtMillis: Long,
    val sizeBytes: Long,
    val createdAt: Long,
)