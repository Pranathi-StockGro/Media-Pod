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

    @Query("DELETE FROM prefetch_chunks WHERE url = :url")
    suspend fun deleteChunksForUrl(url: String)

    @Query("SELECT * FROM prefetch_chunks WHERE url = :url ORDER BY chunkIndex ASC")
    suspend fun getAllChunksForUrl(url: String): List<PrefetchChunkEntity>

    @Query("SELECT * FROM prefetch_metadata")
    suspend fun getAllMetadata(): List<PrefetchEntity>

    @Query("SELECT * FROM prefetch_metadata WHERE createdAt < :timestamp")
    suspend fun getExpiredMetadata(timestamp: Long): List<PrefetchEntity>

    @Query("DELETE FROM prefetch_chunks WHERE downloadedAtMillis < :timestamp")
    suspend fun deleteOldChunks(timestamp: Long)
    
    @Query("DELETE FROM prefetch_metadata WHERE url = :url")
    suspend fun deleteMetadata(url: String)

    @RawQuery
    suspend fun runtimeQuery(query: RoomRawQuery): List<PrefetchEntity>
}
