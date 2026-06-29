package com.stockgro.prefetch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "prefetch_chunks",
    primaryKeys = ["url", "chunkIndex"],
    foreignKeys = [
        ForeignKey(
            entity = PrefetchEntity::class,
            parentColumns = ["url"],
            childColumns = ["url"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["url"])]
)
data class PrefetchChunkEntity(
    val url: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val localFilePath: String,
    val isCompleted: Boolean,
    val downloadedAtMillis: Long
)
