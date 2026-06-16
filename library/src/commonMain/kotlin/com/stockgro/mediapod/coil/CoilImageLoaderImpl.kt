package com.stockgro.mediapod.coil

import coil3.PlatformContext
import coil3.compose.asPainter
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.ImageRequestDisposable
import com.stockgro.mediapod.ImageResult
import com.stockgro.mediapod.PlatformImage
import com.stockgro.mediapod.coil.mappers.buildCoilLoader
import com.stockgro.mediapod.coil.mappers.toCoilRequest
import com.stockgro.mediapod.coil.mappers.toDataSource
import com.stockgro.mediapod.enums.CachePolicy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CoilImageLoaderImpl(
    private val context: PlatformContext,
) : ImageLoader {

    private var coilLoader: coil3.ImageLoader = buildCoilLoader(ImageLoaderConfig.default(), context)

    private val scope = CoroutineScope(Dispatchers.Default)


    override suspend fun execute(request: ImageRequest): ImageResult {
        val coilRequest = request.toCoilRequest(context)
        return when (val result = coilLoader.execute(coilRequest)) {
            is SuccessResult -> ImageResult.Success(
                drawable = result.image.toPlatformImage(context),
                dataSource = result.dataSource.toDataSource(),
                request = request,
            )

            is ErrorResult -> ImageResult.Error(
                throwable = result.throwable,
                request = request,
            )
        }
    }

    override fun enqueue(request: ImageRequest): ImageRequestDisposable {
        val deferred = CompletableDeferred<ImageResult>()
        val coilRequest = request.toCoilRequest(context)
        val coilDisposable = coilLoader.enqueue(coilRequest)

        scope.launch {
            try {
                deferred.await()
                // already completed by the listener set in toCoilRequest
            } catch (_: Exception) { /* cancelled */
            }
        }

        // Bridge Coil's disposable to our interface
        return object : ImageRequestDisposable {
            override val isDisposed: Boolean
                get() = coilDisposable.isDisposed

            override fun dispose() {
                coilDisposable.dispose()
                deferred.cancel()
            }

            override suspend fun await(): ImageResult {
                return when (val r = coilDisposable.job.await()) {
                    is SuccessResult -> ImageResult.Success(
                        drawable = r.image.toPlatformImage(context),
                        dataSource = r.dataSource.toDataSource(),
                        request = request,
                    )

                    is ErrorResult -> ImageResult.Error(
                        throwable = r.throwable,
                        request = request,
                    )
                }
            }
        }
    }

    override fun configure(config: ImageLoaderConfig) {
        coilLoader.shutdown()
        coilLoader = buildCoilLoader(config, context)
    }

    override suspend fun prefetch(data: Any): ImageResult {
        val request = ImageRequest.Builder(data)
            .diskCachePolicy(CachePolicy.WRITE_ONLY)
            .build()
        return execute(request)
    }

    override fun clearMemoryCache() {
        coilLoader.memoryCache?.clear()
    }

    override suspend fun clearDiskCache() {
        coilLoader.diskCache?.clear()
    }

    override fun shutdown() {
        coilLoader.shutdown()
    }

}

private fun coil3.Image.toPlatformImage(context: PlatformContext): PlatformImage = platformImageFrom(this, context)

fun platformImageFrom(image: coil3.Image, context: PlatformContext): PlatformImage {
    return PlatformImage(image.asPainter(context))
}
