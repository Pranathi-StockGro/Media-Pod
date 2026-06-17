package com.stockgro.mediapod.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.stockgro.mediapod.AsyncImageState
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.drawableOrNull

@Composable
fun MPImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = ImageLoaderProvider.default,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    loadingPlaceholder: @Composable (() -> Unit)? = null,
    errorPlaceholder: @Composable (() -> Unit)? = null,
) {
    val state = rememberAsyncImageState(data = data, imageLoader = imageLoader)
    LaunchedEffect(state) {
        println(state)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is AsyncImageState.Empty,
            is AsyncImageState.Loading -> {
                loadingPlaceholder?.invoke()
            }

            is AsyncImageState.Error -> {
                errorPlaceholder?.invoke()
            }

            is AsyncImageState.Success -> {
                state.result.drawableOrNull?.let {
                    Image(
                        painter = it.painter,
                        contentDescription = contentDescription,
                        contentScale = contentScale,
                        alignment = alignment,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}