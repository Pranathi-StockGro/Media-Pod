package com.stockgro.mediapod

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AsyncImageState {
    object Empty : AsyncImageState
    data class Loading(val placeholder: PlatformImage? = null) : AsyncImageState
    data class Success(val result: ImageResult) : AsyncImageState
    data class Error(
        val error: Throwable,
        val errorImage: PlatformImage? = null
    ) : AsyncImageState
}