package com.stockgro.mediapod.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import com.stockgro.mediapod.AsyncImageState
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.drawableOrNull
import com.stockgro.mediapod.utils.RequestSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

@Composable
fun MPImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = ImageLoaderProvider.default,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
    fallback: @Composable (() -> Unit)? = null,
    requestBuilder: (ImageRequest.Builder.() -> Unit)? = null,
) {

    val sizeResolver = remember { ConstraintsSizeResolver() }

    val state = rememberAsyncImageState(
        data = data ?: "",
        imageLoader = imageLoader,
        sizeResolver = sizeResolver,
    ) {
        requestBuilder?.invoke(this)
    }

    val sizeModifier = modifier.then(sizeResolver)

    when {
        data == null -> {
            Box(modifier = sizeModifier, contentAlignment = alignment) {
                fallback?.invoke() ?: error?.invoke() ?: placeholder?.invoke()
            }
        }

        state is AsyncImageState.Empty || state is AsyncImageState.Loading -> {
            Box(modifier = sizeModifier, contentAlignment = alignment) {
                placeholder?.invoke()
            }
        }

        state is AsyncImageState.Error -> {
            Box(modifier = sizeModifier, contentAlignment = alignment) {
                error?.invoke() ?: placeholder?.invoke()
            }
        }

        state is AsyncImageState.Success -> {
            val platformImage = state.result.drawableOrNull
            if (platformImage != null) {
                Image(
                    painter = platformImage.painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    modifier = sizeModifier,
                )
            } else {
                Box(modifier = sizeModifier, contentAlignment = alignment) {
                    error?.invoke() ?: placeholder?.invoke()
                }
            }
        }
    }
}


@Stable
class ConstraintsSizeResolver : LayoutModifier {
    private val sizeFlow = MutableStateFlow<RequestSize.Fixed?>(null)

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0

        if (width > 0 && height > 0) {
            sizeFlow.value = RequestSize.Fixed(width, height)
        }

        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    suspend fun resolveSize(): RequestSize.Fixed {
        return sizeFlow.mapNotNull { it }.first()
    }
}
