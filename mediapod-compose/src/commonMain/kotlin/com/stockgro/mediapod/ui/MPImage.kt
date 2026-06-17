package com.stockgro.mediapod.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.stockgro.mediapod.AsyncImageState
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.drawableOrNull

@Composable
fun MPImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier, // Passed directly to the primary drawing component
    imageLoader: ImageLoader = ImageLoaderProvider.default,
    contentScale: ContentScale = ContentScale.Crop, // Crop defaults ensure edges fill properly
    alignment: Alignment = Alignment.Center,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
    fallback: @Composable (() -> Unit)? = null,
    requestBuilder: (ImageRequest.Builder.() -> Unit)? = null,
) {
    val state = rememberAsyncImageState(data = data, imageLoader = imageLoader) {
        requestBuilder?.invoke(this)
    }

    when (state) {
        is AsyncImageState.Empty,
        is AsyncImageState.Loading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                placeholder?.invoke()
            }
        }

        is AsyncImageState.Error -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                if (fallback != null) fallback() else if (error != null) error() else placeholder?.invoke()
            }
        }

        is AsyncImageState.Success -> {
            state.result.drawableOrNull?.let {
                Image(
                    painter = it.painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    modifier = modifier
                )
            }
        }
    }
}
