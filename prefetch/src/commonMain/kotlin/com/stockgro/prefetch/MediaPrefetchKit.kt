package com.stockgro.prefetch

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.stockgro.prefetch.data.PrefetchDatabase
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.io.files.Path

object MediaPrefetchKit {

    internal fun create(
        databaseBuilder: RoomDatabase.Builder<PrefetchDatabase>,
        cachePath: Path,
        httpClient: HttpClient
    ): MediaPrefetchManager {

        val database = databaseBuilder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        return MediaPrefetchManager(
            httpClient = httpClient,
            database = database,
            cacheDirectoryPath = cachePath
        )
    }
}