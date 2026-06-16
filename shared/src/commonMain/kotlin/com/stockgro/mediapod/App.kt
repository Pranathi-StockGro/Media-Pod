package com.stockgro.mediapod

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stockgro.mediapod.coil.SetSingletonCoilImageLoaderFactory
import com.stockgro.mediapod.ui.MPImage

@Composable
@Preview
fun App() {
    MaterialTheme {
        SetSingletonCoilImageLoaderFactory(
            ImageLoaderConfig.Builder()
                .memoryCache {
                    enabled(true)
                    maxSizePercent(0.25)
                }
                .diskCache {
                    enabled(true)
                    maxSizeBytes(100L * 1024 * 1024)
                }
                .network {
                    connectTimeoutMillis(10_000)
                    readTimeoutMillis(30_000)
                }
                .respectCacheHeaders(true)
                .build()
        )

        MPImage(
            modifier = Modifier.fillMaxSize(),
            data = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&h=400&q=80",
            contentDescription = null
        )
    }
}