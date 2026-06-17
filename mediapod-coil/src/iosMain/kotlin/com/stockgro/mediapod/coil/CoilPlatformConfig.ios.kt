package com.stockgro.mediapod.coil

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.stockgro.mediapod.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual object CoilPlatformConfig {

    private var sharedHttpClient: HttpClient? = null

    @OptIn(ExperimentalCoilApi::class)
    actual fun applyNetworkFetcher(
        builder: ImageLoader.Builder,
        config: NetworkConfig,
        respectCacheHeaders: Boolean
    ) {
        val client = sharedHttpClient ?: HttpClient(Darwin) {
            // Configuration can be added here if needed to match NetworkConfig
        }.also { sharedHttpClient = it }

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
