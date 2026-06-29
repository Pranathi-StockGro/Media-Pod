package com.stockgro.prefetch

import android.content.Context
import androidx.room.Room
import com.stockgro.prefetch.data.PrefetchDatabase
import io.ktor.client.HttpClient
import kotlinx.io.files.Path

fun MediaPrefetchKit.initialize(
    context: Context,
    httpClient: HttpClient,
    interceptors: PrefetchInterceptor? = null
): MediaPrefetchManager {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("prefetch_media.db")

    val dbBuilder = Room.databaseBuilder<PrefetchDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration(true)
    val cachePath = Path(appContext.cacheDir.absolutePath, "prefetch-media")

    return create(dbBuilder, cachePath, httpClient, interceptors)
}