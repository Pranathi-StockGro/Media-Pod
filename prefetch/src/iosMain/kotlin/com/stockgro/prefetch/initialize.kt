package com.stockgro.prefetch

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.stockgro.prefetch.data.PrefetchDatabase
import com.stockgro.prefetch.data.PrefetchDatabaseConstructor
import io.ktor.client.HttpClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

fun MediaPrefetchKit.initialize(
    httpClient: HttpClient,
    interceptors: PrefetchInterceptor? = null
): MediaPrefetchManager {

    val dbFilePath = getDatabasePath() + "/prefetch_media.db"

    val dbBuilder = Room.databaseBuilder<PrefetchDatabase>(
        name = dbFilePath,
        factory = { PrefetchDatabaseConstructor.initialize() }
    ).setDriver(BundledSQLiteDriver())
     .fallbackToDestructiveMigration(true)

    val cachePath = resolveCacheDirectory()
    return create(dbBuilder, cachePath, httpClient, interceptors)
}

@OptIn(ExperimentalForeignApi::class)
private fun getDatabasePath(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}

fun resolveCacheDirectory(): Path {
    val cachesDirectory = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
    return Path(cachesDirectory, "prefetch-media")
}