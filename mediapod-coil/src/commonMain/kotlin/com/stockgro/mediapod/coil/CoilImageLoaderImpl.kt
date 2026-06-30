package com.stockgro.mediapod.coil

import coil3.PlatformContext
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
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile


class CoilImageLoaderImpl(
    private val context: PlatformContext,
) : ImageLoader {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    @Volatile
    private var coilLoader: coil3.ImageLoader = buildCoilLoader(ImageLoaderConfig.default(), context)

    override suspend fun execute(request: ImageRequest): ImageResult {

        val coilRequest = request.toCoilRequest(context)

        return when (val result = coilLoader.execute(coilRequest)) {
            is SuccessResult -> {
                ImageResult.Success(
                    drawable = result.image.toPlatformImage(context),
                    dataSource = result.dataSource.toDataSource(),
                    request = request,
                )
            }

            is ErrorResult -> {
                ImageResult.Error(throwable = result.throwable, request = request)
            }
        }
    }

    override fun enqueue(request: ImageRequest): ImageRequestDisposable {
        val currentLoader = coilLoader
        val deferred = CompletableDeferred<ImageResult>()
        val coilRequest = request.toCoilRequest(context)
        val coilDisposable = currentLoader.enqueue(coilRequest)

        val trackingJob = mainScope.launch {
            try {
                when (val r = coilDisposable.job.await()) {
                    is SuccessResult -> {
                        deferred.complete(
                            ImageResult.Success(
                                drawable = r.image.toPlatformImage(context),
                                dataSource = r.dataSource.toDataSource(),
                                request = request,
                            )
                        )
                    }
                    is ErrorResult -> {
                        deferred.complete(
                            ImageResult.Error(throwable = r.throwable, request = request)
                        )
                    }
                }
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            }
        }

        return object : ImageRequestDisposable {
            override val isDisposed: Boolean
                get() = coilDisposable.isDisposed || trackingJob.isCompleted

            override fun dispose() {
                coilDisposable.dispose()
                trackingJob.cancel()
                deferred.cancel()
            }

            override suspend fun await(): ImageResult {
                return deferred.await()
            }
        }
    }

    override fun configure(config: ImageLoaderConfig) {
        val oldLoader = coilLoader
        coilLoader = buildCoilLoader(config, context)
        oldLoader.shutdown()
    }

    override suspend fun prefetch(data: Any): ImageResult {
        val currentLoader = coilLoader
        val request = ImageRequest.Builder(data)
            .diskCachePolicy(CachePolicy.WRITE_ONLY)
            .build()
        val coilRequest = request.toCoilRequest(context)
        return when (val result = currentLoader.execute(coilRequest)) {
            is SuccessResult -> ImageResult.Success(
                drawable = result.image.toPlatformImage(context),
                dataSource = result.dataSource.toDataSource(),
                request = request,
            )
            is ErrorResult -> ImageResult.Error(throwable = result.throwable, request = request)
        }
    }

    override fun clearMemoryCache() {
        coilLoader.memoryCache?.clear()
    }

    override suspend fun clearDiskCache() {
        withContext(Dispatchers.IO) {
            coilLoader.diskCache?.clear()
        }
    }

    override fun shutdown() {
        coilLoader.shutdown()
    }
}

/**
 * Common extension to transform a Coil Image into our engine-agnostic wrapper.
 */
internal expect fun coil3.Image.toPlatformImage(context: PlatformContext): PlatformImage

/**
 * Common factory function to map a Coil Image.
 */
internal expect fun platformImageFrom(image: coil3.Image, context: PlatformContext): PlatformImage
