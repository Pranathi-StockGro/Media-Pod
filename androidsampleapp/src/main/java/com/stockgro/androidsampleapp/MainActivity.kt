package com.stockgro.androidsampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stockgro.androidsampleapp.ui.theme.MediaPodTheme
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.glide.SetGlideImageLoaderFactory
import com.stockgro.mediapod.coil.SetCoilImageLoaderFactory
import com.stockgro.mediapod.ui.MPImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
//            SetCoilImageLoaderFactory(
//                ImageLoaderConfig.Builder()
//                    .memoryCache { maxSizePercent(0.25) }
//                    .diskCache { maxSizeBytes(100L * 1024 * 1024) }
//                    .build()
//            )

            SetGlideImageLoaderFactory(
                this,
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
            MediaPodTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MPImage(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        data = "https://cdn.stockgro.com/experts_assets/icons/ice.png",
                        contentDescription = null,
                        placeholder = {
                            Text("placeholder")
                        },
                        error = {
                            Text("error loading image")
                        },
                        fallback = {
                            Text("image not found")
                        }
                    )
                }
            }
        }
    }
}