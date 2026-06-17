package com.stockgro.mediapod.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stockgro.mediapod.AsyncImageState
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.ImageResult
import kotlin.coroutines.cancellation.CancellationException


@Composable
fun rememberAsyncImageState(
    data: Any?,
    imageLoader: ImageLoader = ImageLoaderProvider.default
): AsyncImageState {
    var state by remember(data) {
        mutableStateOf(if (data == null) AsyncImageState.Empty else AsyncImageState.Loading)
    }

    if (data == null) {
        return AsyncImageState.Empty
    }

    LaunchedEffect(data, imageLoader) {
        state = AsyncImageState.Loading
        try {
            val request = ImageRequest.Builder(data).build()
            val result = imageLoader.execute(request)
            state = when (result) {
                is ImageResult.Error -> AsyncImageState.Error(result.throwable)
                is ImageResult.Success -> AsyncImageState.Success(result)
            }

        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            state = AsyncImageState.Error(e)
        }
    }

    return state
}