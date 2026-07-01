package com.stockgro.androidsampleapp

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.Transformation
import com.stockgro.mediapod.coil.setupCoilImageLoader
import com.stockgro.mediapod.glide.setupGlideImageLoader
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

//        this.setupCoilImageLoader(config)
        this.setupGlideImageLoader(config)

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imageView = findViewById<ImageView>(R.id.my_image_view)

        // 2. Call your beautiful, non-Compose load extension function!
        imageView.load("https://cdn.stockgro.com/experts_assets/icons/ice.png")
        {
            crossfade(true)
            crossfade(500)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_foreground)
            transformations(
                Transformation.CircleCrop,
//                Transformation.RoundedCorners(30f),
            )
        }
    }
}