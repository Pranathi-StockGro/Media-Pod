package com.stockgro.mediapod.coil

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.stockgro.mediapod.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

actual object CoilPlatformConfig {
    /**
     * A lazily-created shared HttpClient.
     *
     * Replace this with your app's singleton HttpClient if you manage one
     * centrally (e.g. via Dagger / Hilt / Koin).
     */
    var platformHttpClient: HttpClient? = null
        private set

    @OptIn(ExperimentalCoilApi::class)
    actual fun applyNetworkFetcher(
        builder: ImageLoader.Builder,
        config: NetworkConfig,
        respectCacheHeaders: Boolean
    ) {
        val client = platformHttpClient ?: HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = config.readTimeoutMillis
                connectTimeoutMillis = config.connectTimeoutMillis
                socketTimeoutMillis = config.readTimeoutMillis
            }
        }.also { platformHttpClient = it }

        builder.components {
            add(
                KtorNetworkFetcherFactory(
                    httpClient = { client },
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