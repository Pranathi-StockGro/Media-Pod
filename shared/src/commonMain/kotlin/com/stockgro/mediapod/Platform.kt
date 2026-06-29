package com.stockgro.mediapod

import androidx.compose.runtime.Composable
import com.stockgro.prefetch.MediaPrefetchManager

@Composable
expect fun rememberPrefetchManager(): MediaPrefetchManager
