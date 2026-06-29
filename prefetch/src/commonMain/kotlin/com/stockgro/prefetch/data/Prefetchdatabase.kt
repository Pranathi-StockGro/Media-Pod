package com.stockgro.prefetch.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [PrefetchEntity::class, PrefetchChunkEntity::class], version = 1)
@ConstructedBy(PrefetchDatabaseConstructor::class)
abstract class PrefetchDatabase : RoomDatabase() {
    abstract fun prefetchDao(): PrefetchDao
}

@Suppress("KotlinNoActualForExpect")
expect object PrefetchDatabaseConstructor : RoomDatabaseConstructor<PrefetchDatabase> {
    override fun initialize(): PrefetchDatabase
}
