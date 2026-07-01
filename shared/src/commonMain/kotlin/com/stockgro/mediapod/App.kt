package com.stockgro.mediapod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stockgro.mediapod.coil.setupCoilImageLoader
import com.stockgro.mediapod.ui.MPImage
import com.stockgro.mediapod.viewmodel.VideoViewModel

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf("image") }
    val prefetchManager = rememberPrefetchManager()
    val videoViewModel = remember(prefetchManager) { VideoViewModel(prefetchManager) }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { currentScreen = "image" },
                    colors = if (currentScreen == "image") ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("Images")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { currentScreen = "video" },
                    colors = if (currentScreen == "video") ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("Videos")
                }
            }

            HorizontalDivider()

            Box(modifier = Modifier.weight(1f)) {
                if (currentScreen == "image") {
                    ImageTestScreen()
                } else {
                    VideoApp(viewModel = videoViewModel)
                }
            }
        }
    }
}

@Composable
fun ImageTestScreen() {
    setupCoilImageLoader(
        ImageLoaderConfig.Builder()
            .memoryCache { maxSizePercent(0.25) }
            .diskCache { maxSizeBytes(100L * 1024 * 1024) }
            .build()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MediaPod Transformations Test", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        // 1. Basic Image
        TestSection("1. Basic Image") {
            MPImage(
                data = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                contentDescription = null,
                modifier = Modifier.size(150.dp)
            )
        }

        // 2. Circle Crop via Modifier
        TestSection("2. Circle Crop (Modifier)") {
            MPImage(
                data = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400",
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // 3. Rounded Corners via Modifier
        TestSection("3. Rounded Corners (Modifier)") {
            MPImage(
                data = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // 4. Blur via Modifier
        TestSection("4. Blur (Modifier)") {
            MPImage(
                data = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .blur(8.dp)
            )
        }

        // 5. Placeholder & Error
        TestSection("5. Custom Composable Placeholders") {
            MPImage(
                data = "https://invalid-url.com/nothing.jpg",
                contentDescription = null,
                modifier = Modifier.size(150.dp),
                placeholder = {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                },
                error = {
                    Text("Failed to load!", color = Color.Red)
                }
            )
        }
    }
}

@Composable
fun TestSection(title: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
