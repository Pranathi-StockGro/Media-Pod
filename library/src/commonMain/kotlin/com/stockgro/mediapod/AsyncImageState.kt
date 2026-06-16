package com.stockgro.mediapod

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AsyncImageState {
    object Empty : AsyncImageState
    object Loading : AsyncImageState
    data class Success(val result: ImageResult) : AsyncImageState
    data class Error(val error: Throwable) : AsyncImageState // Adjust based on your ImageResult structure
}