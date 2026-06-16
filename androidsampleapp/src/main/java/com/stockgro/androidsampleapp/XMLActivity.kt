package com.stockgro.androidsampleapp

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.coil.initializeCoilImageLoader
import com.stockgro.mediapod.view.load

class XMLActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val config = ImageLoaderConfig.Builder()
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

        this.initializeCoilImageLoader(config)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imageView = findViewById<ImageView>(R.id.my_image_view)

        // 2. Call your beautiful, non-Compose load extension function!
        imageView.load("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&h=400&q=80") {
            crossfade(true)
            crossfade(500)
        }
    }
}