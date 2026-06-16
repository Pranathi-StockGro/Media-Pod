package com.stockgro.mediapod.coil

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.stockgro.mediapod.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import io.ktor.client.engine.darwin.Darwin

actual object CoilPlatformConfig {

    @OptIn(ExperimentalCoilApi::class)
    actual fun applyNetworkFetcher(
        builder: ImageLoader.Builder,
        config: NetworkConfig,
        respectCacheHeaders: Boolean
    ) {
        val client = HttpClient(Darwin) {
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMillis
                requestTimeoutMillis = config.readTimeoutMillis
            }
        }
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