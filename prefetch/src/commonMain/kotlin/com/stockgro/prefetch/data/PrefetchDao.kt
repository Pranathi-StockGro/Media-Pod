package com.stockgro.prefetch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery

@Dao
interface PrefetchDao {
    @Query("SELECT * FROM prefetch_metadata WHERE url = :url")
    suspend fun getMetadata(url: String): PrefetchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(entity: PrefetchEntity)

    @Query("SELECT * FROM prefetch_chunks WHERE url = :url AND chunkIndex = :index")
    suspend fun getChunk(url: String, index: Int): PrefetchChunkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: PrefetchChunkEntity)

    /**
     * Deletes a specific chunk from the database.
     */
    @Query("DELETE FROM prefetch_chunks WHERE url = :url AND chunkIndex = :index")
    suspend fun deleteChunk(url: String, index: Int)

    @Query("DELETE FROM prefetch_chunks WHERE url = :url")
    suspend fun deleteChunksForUrl(url: String)

    @Query("SELECT * FROM prefetch_chunks WHERE url = :url ORDER BY chunkIndex ASC")
    suspend fun getAllChunksForUrl(url: String): List<PrefetchChunkEntity>

    @Query("SELECT * FROM prefetch_metadata")
    suspend fun getAllMetadata(): List<PrefetchEntity>

    @Query("SELECT * FROM prefetch_metadata WHERE createdAt < :timestamp")
    suspend fun getExpiredMetadata(timestamp: Long): List<PrefetchEntity>

    /**
     * Deletes chunks older than the specified timestamp.
     */
    @Query("DELETE FROM prefetch_chunks WHERE downloadedAtMillis < :timestamp")
    suspend fun deleteOldChunks(timestamp: Long)
    
    @Query("DELETE FROM prefetch_metadata WHERE url = :url")
    suspend fun deleteMetadata(url: String)

    /**
     * Calculates the total size of all completed chunks in the cache.
     */
    @Query("SELECT SUM(endByte - startByte + 1) FROM prefetch_chunks WHERE isCompleted = 1")
    suspend fun getTotalCacheSize(): Long?

    /**
     * Retrieves all completed chunks ordered by download time (oldest first).
     * Used for LRU cache eviction.
     */
    @Query("SELECT * FROM prefetch_chunks WHERE isCompleted = 1 ORDER BY downloadedAtMillis ASC")
    suspend fun getOldestChunks(): List<PrefetchChunkEntity>

    @RawQuery
    suspend fun runtimeQuery(query: RoomRawQuery): List<PrefetchEntity>
}
