package com.stockgro.mediapod.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.stockgro.mediapod.DiskCacheConfig
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.MemoryCacheConfig
import java.io.InputStream

/**
 * Internal helper to convert [com.stockgro.mediapod.ImageLoaderConfig] into Glide-specific configurations.
 *
 * These extension functions are used by [MediaPodGlideModule] to apply configurations
 * during Glide's initialization.
 */
object GlideImageLoaderConfig {

    fun applyConfigAndInitialize(context: Context, config: ImageLoaderConfig) {
        val appContext = context.applicationContext

        val builder = GlideBuilder()
        GlideOkHttpConfig.getOrBuildClient(config.networkConfig)
        builder.applyMemoryCache(appContext, config.memoryCache)
        builder.applyDiskCache(appContext, config.diskCache)
        Glide.init(appContext, builder)

        val networkConfig = com.stockgro.mediapod.NetworkConfig.Builder().build()

        val client = GlideOkHttpConfig.getOrBuildClient(networkConfig)

        val glideInstance = Glide.get(appContext)
        val registry = glideInstance.registry

        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }

    // ── Memory cache builder ──────────────────────────────────────────────────

    private fun GlideBuilder.applyMemoryCache(
        context: Context,
        config: MemoryCacheConfig,
    ) {
        if (!config.enabled) {
            setMemoryCache(LruResourceCache(0))
            return
        }

        val memoryCacheSizeBytes: Long = when {
            config.maxSizeBytes > 0 -> config.maxSizeBytes
            else -> {
                val calculator = MemorySizeCalculator.Builder(context)
                    .setMemoryCacheScreens(1f)   // base for percent scaling
                    .build()
                (calculator.memoryCacheSize * config.maxSizePercent / 0.4).toLong()
            }
        }

        setMemoryCache(LruResourceCache(memoryCacheSizeBytes))
    }

    // ── Disk cache builder ────────────────────────────────────────────────────

    private fun GlideBuilder.applyDiskCache(
        context: Context,
        config: DiskCacheConfig,
    ) {
        if (!config.enabled) {
            setDiskCache {
                com.bumptech.glide.disklrucache.DiskLruCache.open(
                    context.cacheDir,
                    1,
                    1,
                    1L,
                )
                null
            }
            return
        }

        val diskFactory = when (val dir = config.directory) {
            null -> InternalCacheDiskCacheFactory(context, config.maxSizeBytes)
            else -> DiskLruCacheFactory(dir, config.maxSizeBytes)
        }

        setDiskCache(diskFactory)
    }
}