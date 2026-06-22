package com.stockgro.prefetch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrefetchDao {
    @Query("SELECT * FROM media_prefetch_registry WHERE url = :url")
    suspend fun getEntry(url: String): PrefetchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PrefetchEntity)

    @Query("DELETE FROM media_prefetch_registry WHERE url = :url")
    suspend fun deleteEntry(url: String)

    @Query("SELECT * FROM media_prefetch_registry")
    suspend fun getAllEntries(): List<PrefetchEntity>

    @Query("SELECT * FROM media_prefetch_registry WHERE createdAt < :timestamp")
    suspend fun getExpiredEntries(timestamp: Long): List<PrefetchEntity>
}