package com.stockgro.mediapod.coil.mappers

import coil3.ImageLoader
import coil3.PlatformContext
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.coil.CoilPlatformConfig
import okio.Path.Companion.toPath


internal fun buildCoilLoader(config: ImageLoaderConfig, context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCache {
            if (!config.memoryCache.enabled) {
                null // Returning null completely disables the memory cache layer
            } else {
                coil3.memory.MemoryCache.Builder()
                    .apply {
                        if (config.memoryCache.maxSizeBytes > 0) {
                            maxSizePercent(context, 0.0)
                            maxSizeBytes { config.memoryCache.maxSizeBytes }
                        } else {
                            maxSizePercent(context, config.memoryCache.maxSizePercent)
                        }
                    }
                    .build()
            }
        }
        .diskCache {
            coil3.disk.DiskCache.Builder().apply {
                if (!config.diskCache.enabled) {
                    maxSizeBytes(0)
                } else {
                    maxSizeBytes(config.diskCache.maxSizeBytes)
                    if (config.diskCache.directory != null) {
                        directory(requireNotNull(config.diskCache.directory?.toPath()))
                    } else {
                        val defaultPath = okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "mediapod_disk_cache"
                        directory(defaultPath)
                    }
                }
            }.build()
        }
        .apply {
            CoilPlatformConfig.applyNetworkFetcher(this, config.networkConfig, config.respectCacheHeaders)
        }
        // ── Logger ───────────────────────────────────────────────────────
//        .apply {
//            config.logger?.let { appLogger ->
//                logger(CoilLoggerBridge(appLogger))
//            }
//        }
        .build()
}
