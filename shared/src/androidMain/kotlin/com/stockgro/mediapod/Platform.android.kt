package com.stockgro.mediapod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.stockgro.prefetch.MediaPrefetchKit
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.initialize
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

@Composable
actual fun rememberPrefetchManager(): MediaPrefetchManager {
    val context = LocalContext.current
    return remember {
        val httpClient = HttpClient(Android)
        MediaPrefetchKit.initialize(context, httpClient)
    }
}
