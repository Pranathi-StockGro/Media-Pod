package com.stockgro.mediapod.glide

import com.stockgro.mediapod.NetworkConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object GlideOkHttpConfig {

    @Volatile
    var okHttpClient: OkHttpClient? = null
        private set

    fun setOkHttpClient(client: OkHttpClient) {
        okHttpClient = client
    }

    fun getOrBuildClient(networkConfig: NetworkConfig): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(networkConfig.connectTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(networkConfig.readTimeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(networkConfig.writeTimeoutMillis, TimeUnit.MILLISECONDS)
                .build().also { okHttpClient = it }
        }
    }
}