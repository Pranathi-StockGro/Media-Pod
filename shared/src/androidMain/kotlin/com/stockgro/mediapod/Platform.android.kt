package com.stockgro.mediapod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.stockgro.prefetch.MediaPrefetchKit
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.initialize
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

@Composable
actual fun rememberPrefetchManager(): MediaPrefetchManager {
    val context = LocalContext.current
    return remember {
        val httpClient = HttpClient(Android) {
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000   // 30 seconds
                socketTimeoutMillis = 180_000    // 3 minutes
                requestTimeoutMillis = 60_000   // 60 seconds
            }
        }
        MediaPrefetchKit.initialize(context, httpClient)
    }
}
