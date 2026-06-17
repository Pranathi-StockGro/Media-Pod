package com.stockgro.mediapod.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.LibraryGlideModule
import com.stockgro.mediapod.DiskCacheConfig
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.MemoryCacheConfig
import java.io.InputStream
import kotlin.jvm.java


object GlideImageLoaderConfig {
    fun applyToGlide(context: Context, config: ImageLoaderConfig) {
        val appContext = context.applicationContext
        Glide.tearDown()

        val builder = GlideBuilder()

        GlideOkHttpConfig.getOrBuildClient(config.networkConfig)
        builder.applyMemoryCache(appContext, config.memoryCache)
        builder.applyDiskCache(appContext, config.diskCache)
        Glide.init(appContext, builder)
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

@GlideModule
class ImageLoaderLibraryGlideModule : LibraryGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val networkConfig = com.stockgro.mediapod.NetworkConfig.Builder().build()

        val client = GlideOkHttpConfig.getOrBuildClient(networkConfig)

        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }
}

internal object GlideOkHttpConfig {

    @Volatile
    var okHttpClient: okhttp3.OkHttpClient? = null
        private set

    fun setOkHttpClient(client: okhttp3.OkHttpClient) {
        okHttpClient = client
    }

    fun getOrBuildClient(networkConfig: com.stockgro.mediapod.NetworkConfig): okhttp3.OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: okhttp3.OkHttpClient.Builder()
                .connectTimeout(networkConfig.connectTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(networkConfig.readTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(networkConfig.writeTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build().also { okHttpClient = it }
        }
    }
}