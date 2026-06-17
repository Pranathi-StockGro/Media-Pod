package com.stockgro.mediapod.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderConfig
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.ImageRequestDisposable
import com.stockgro.mediapod.ImageResult
import com.stockgro.mediapod.ImageSource
import com.stockgro.mediapod.PlatformImage
import com.stockgro.mediapod.enums.CachePolicy
import com.stockgro.mediapod.enums.DataSource
import com.stockgro.mediapod.utils.RequestSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bumptech.glide.load.DataSource as GlideDataSource

/**
 * [ImageLoader] implementation backed by **Glide 5** (Android only).
 *
 * Glide manages its own singleton internally via [Glide.with()]. This class
 * bridges our [ImageRequest] / [ImageLoaderConfig] contracts to Glide's
 * builder API without leaking any Glide types to callers.
 *
 * Usage:
 * ```kotlin
 * val loader = GlideImageLoader(context)
 * loader.configure(
 *     ImageLoaderConfig.Builder()
 *         .memoryCache { maxSizePercent(0.25) }
 *         .diskCache   { maxSizeBytes(100L * 1024 * 1024) }
 *         .build()
 * )
 * ImageLoaderProvider.setDefault(loader)
 * ```
 *
 * @param context Application context — never store an Activity context here.
 */
class GlideImageLoaderImpl(private val context: Context) : ImageLoader {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val requestManager: RequestManager
        get() = Glide.with(context.applicationContext)

    override fun configure(config: ImageLoaderConfig) {
        GlideImageLoaderConfig.applyToGlide(context, config)
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        // FIX 1: Change to GlideFetchResult to match buildRequestInto
        val deferred = CompletableDeferred<GlideFetchResult>()

        request.target?.onStart(null)
        val target = buildRequestInto(request, deferred)

        return try {
            val fetchResult = deferred.await()

            // Invoke the secondary constructor to pass both the Painter and the raw Drawable
            val platformImage = PlatformImage(
                painter = AndroidDrawablePainter(fetchResult.drawable),
                nativeDrawable = fetchResult.drawable
            )

            request.target?.onSuccess(platformImage)

            ImageResult.Success(
                drawable = platformImage,
                dataSource = fetchResult.dataSource,
                request = request
            )
        } catch (e: Exception) {
            request.target?.onError(null)
            ImageResult.Error(throwable = e, request = request)
        } finally {
            withContext(Dispatchers.Main) {
                requestManager.clear(target)
            }
        }
    }

    override fun enqueue(request: ImageRequest): ImageRequestDisposable {
        // FIX 2: Change to GlideFetchResult to match buildRequestInto
        val deferred = CompletableDeferred<GlideFetchResult>()

        request.target?.onStart(null)
        val target = buildRequestInto(request, deferred)

        val job = scope.launch {
            try {
                val fetchResult = deferred.await()

                // Construct with both references so ImageViewTarget can see nativeDrawable
                val platformImage = PlatformImage(
                    painter = AndroidDrawablePainter(fetchResult.drawable),
                    nativeDrawable = fetchResult.drawable
                )

                request.target?.onSuccess(platformImage)
            } catch (_: Exception) {
                request.target?.onError(null)
            }
        }

        return object : ImageRequestDisposable {
            override val isDisposed: Boolean
                get() = job.isCancelled || job.isCompleted

            override fun dispose() {
                job.cancel()
                Handler(Looper.getMainLooper()).post {
                    requestManager.clear(target)
                }
            }

            override suspend fun await(): ImageResult {
                return try {
                    val fetchResult = deferred.await()
                    ImageResult.Success(
                        drawable = PlatformImage(
                            painter = AndroidDrawablePainter(fetchResult.drawable),
                            nativeDrawable = fetchResult.drawable
                        ),
                        dataSource = fetchResult.dataSource,
                        request = request
                    )
                } catch (e: Exception) {
                    ImageResult.Error(throwable = e, request = request)
                }
            }
        }
    }

    override suspend fun prefetch(data: Any): ImageResult {
        val deferred = CompletableDeferred<ImageResult>()
        withContext(Dispatchers.Main) {
            requestManager
                .load(data.toGlideModel())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        deferred.complete(
                            ImageResult.Error(
                                throwable = e ?: Exception("Glide prefetch failed"),
                                request = ImageRequest.Builder(data).build(),
                            )
                        )
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: GlideDataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        deferred.complete(
                            ImageResult.Success(
                                drawable = PlatformImage(AndroidDrawablePainter(resource)),
                                dataSource = dataSource.toDataSource(),
                                request = ImageRequest.Builder(data).build(),
                            )
                        )
                        return true
                    }
                })
                .preload(1, 1)
        }

        return deferred.await()
    }

    override fun clearMemoryCache() {
        Handler(Looper.getMainLooper()).post {
            Glide.get(context).clearMemory()
        }
    }

    override suspend fun clearDiskCache() {
        withContext(Dispatchers.IO) {
            Glide.get(context).clearDiskCache()
        }
    }

    override fun shutdown() {
        requestManager.pauseAllRequests()
    }

    // ── Internal request builder ──────────────────────────────────────────────

    private data class GlideFetchResult(
        val drawable: Drawable,
        val dataSource: DataSource
    )

    private fun buildRequestInto(
        request: ImageRequest,
        deferred: CompletableDeferred<GlideFetchResult>,
    ): Target<Drawable> {

        val target = object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                // Done inside request listener to grab true DataSource details
            }

            override fun onLoadCleared(placeholder: Drawable?) { /* No-op */
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(Exception("Glide load failed for: ${request.data}"))
                }
            }
        }

        val modelToLoad: Any = if (request.headers.isNotEmpty()) {
            val headersBuilder = LazyHeaders.Builder()
            request.headers.forEach { (k, v) -> headersBuilder.addHeader(k, v) }
            GlideUrl(request.data.toString(), headersBuilder.build())
        } else {
            request.data
        }

        Handler(Looper.getMainLooper()).post {
            val options = RequestOptions().apply {
                request.size.applyTo(this)
            }

            var requestBuilder = requestManager
                .load(modelToLoad)
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: GlideDataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (!deferred.isCompleted) {
                            deferred.complete(GlideFetchResult(resource, dataSource.toDataSource()))
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(e ?: Exception("Glide load failed"))
                        }
                        return false
                    }
                })

            if (request.crossfade) {
                requestBuilder = requestBuilder.transition(
                    DrawableTransitionOptions.withCrossFade(request.crossfadeDurationMs)
                )
            }

            requestBuilder.into(target)
        }

        return target
    }
}

// ── GlideDisposable ───────────────────────────────────────────────────────────

private class GlideDisposable(
    private val deferred: CompletableDeferred<ImageResult>,
    private val onDispose: () -> Unit,
) : ImageRequestDisposable {

    @Volatile
    override var isDisposed = false
        private set

    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            deferred.cancel()
            onDispose()
        }
    }

    override suspend fun await(): ImageResult = deferred.await()
}

// ── Mapping helpers ───────────────────────────────────────────────────────────

private fun Any.toGlideModel(): Any = when (this) {
    is ImageSource.Url -> url
    is ImageSource.Resource -> resId
    is ImageSource.LocalFile -> java.io.File(path)
    is ImageSource.Bytes -> data
    else -> this
}

private fun ImageSource.toModel(): Any = when (this) {
    is ImageSource.Resource -> resId
    is ImageSource.Url -> url
    is ImageSource.LocalFile -> java.io.File(path)
    is ImageSource.Bytes -> data
}

fun RequestSize.applyTo(options: RequestOptions) = when (this) {
    is RequestSize.Original -> options.override(Target.SIZE_ORIGINAL)
    is RequestSize.Fixed -> options.override(width, height)
}

//private fun RequestPriority.toGlidePriority() = when (this) {
//    RequestPriority.LOW    -> com.bumptech.glide.load.Priority.LOW
//    RequestPriority.NORMAL -> com.bumptech.glide.load.Priority.NORMAL
//    RequestPriority.HIGH   -> com.bumptech.glide.load.Priority.HIGH
//}

private fun CachePolicy.applyTo(options: RequestOptions) = when (this) {
    CachePolicy.ENABLED -> options.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
    CachePolicy.DISABLED -> options.diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
    CachePolicy.READ_ONLY -> options.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).onlyRetrieveFromCache(true)
    CachePolicy.WRITE_ONLY -> options.diskCacheStrategy(DiskCacheStrategy.DATA).skipMemoryCache(false)
}

private fun GlideDataSource.toDataSource() = when (this) {
    GlideDataSource.MEMORY_CACHE,
    GlideDataSource.RESOURCE_DISK_CACHE -> DataSource.MEMORY_CACHE

    GlideDataSource.DATA_DISK_CACHE -> DataSource.DISK
    GlideDataSource.REMOTE -> DataSource.NETWORK
    GlideDataSource.LOCAL -> DataSource.DISK
}
