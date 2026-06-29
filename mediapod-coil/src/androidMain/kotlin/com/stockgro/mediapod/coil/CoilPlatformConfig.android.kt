package com.stockgro.mediapod.coil

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.stockgro.mediapod.NetworkConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

actual object CoilPlatformConfig {
    /**
     * A lazily-created shared OkHttpClient.
     *
     * Replace this with your app's singleton OkHttpClient if you manage one
     * centrally (e.g. via Dagger / Hilt / Koin).
     */
    var platformHttpClient: OkHttpClient? = null
        private set

    @OptIn(ExperimentalCoilApi::class)
    actual fun applyNetworkFetcher(
        builder: ImageLoader.Builder,
        config: NetworkConfig,
        respectCacheHeaders: Boolean
    ) {
        val client = platformHttpClient ?: OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()
            .also { platformHttpClient = it }

        builder.components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { client },
                    cacheStrategy = {
                        if (respectCacheHeaders) {
                            CacheControlCacheStrategy()
                        } else {
                            CacheStrategy.DEFAULT
                        }
                    }
                )
            )
        }
    }
}