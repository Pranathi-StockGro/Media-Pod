package com.stockgro.mediapod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stockgro.prefetch.MediaPrefetchKit
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.initialize
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

@Composable
actual fun rememberPrefetchManager(): MediaPrefetchManager {
    return remember {
        val httpClient = HttpClient(Darwin)
        MediaPrefetchKit.initialize(httpClient)
    }
}
