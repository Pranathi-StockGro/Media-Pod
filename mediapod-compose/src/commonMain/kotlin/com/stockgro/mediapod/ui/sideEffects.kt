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
import com.stockgro.mediapod.ImageTarget
import com.stockgro.mediapod.PlatformImage
import kotlin.coroutines.cancellation.CancellationException


@Composable
fun rememberAsyncImageState(
    data: Any?,
    imageLoader: ImageLoader = ImageLoaderProvider.default,
    sizeResolver: ConstraintsSizeResolver,
    builder: ImageRequest.Builder.() -> Unit = {}
): AsyncImageState {
    var state by remember(data, builder) {
        mutableStateOf(if (data == null) AsyncImageState.Empty else AsyncImageState.Loading())
    }

    if (data == null) {
        return AsyncImageState.Empty
    }

    LaunchedEffect(data, imageLoader, builder) {
        var errorImg: PlatformImage? = null
        state = AsyncImageState.Loading()
        try {
            val resolvedSize = sizeResolver.resolveSize()

            val request = ImageRequest.Builder(data)
                .size(resolvedSize)
                .imageTarget(object : ImageTarget {
                    override fun onStart(placeholder: PlatformImage?) {
                        state = AsyncImageState.Loading(placeholder)
                    }

                    override fun onError(error: PlatformImage?) {
                        errorImg = error
                    }
                })
                .apply(builder)
                .build()

            val disposable = imageLoader.enqueue(request)
            try {
                val result = disposable.await()
                state = when (result) {
                    is ImageResult.Error -> AsyncImageState.Error(result.throwable, errorImg)
                    is ImageResult.Success -> AsyncImageState.Success(result)
                }
            } finally {
                disposable.dispose()
            }

        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            state = AsyncImageState.Error(e)
        }
    }

    return state
}