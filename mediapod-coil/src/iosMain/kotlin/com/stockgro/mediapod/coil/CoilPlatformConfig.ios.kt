package com.stockgro.mediapod.coil

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.stockgro.mediapod.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

actual object CoilPlatformConfig {

    private var platformHttpClient: HttpClient? = null

    @OptIn(ExperimentalCoilApi::class)
    actual fun applyNetworkFetcher(
        builder: ImageLoader.Builder,
        config: NetworkConfig,
        respectCacheHeaders: Boolean
    ) {
        val client = platformHttpClient ?: HttpClient(Darwin) {
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
