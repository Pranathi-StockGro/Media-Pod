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
import kotlinx.coroutines.Dispatchers
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

    // Glide's RequestManager bound to the Application lifecycle
    private val requestManager: RequestManager
        get() = Glide.with(context.applicationContext)

    override fun configure(config: ImageLoaderConfig) {
        GlideImageLoaderConfig.applyToGlide(context, config)
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        val deferred = CompletableDeferred<ImageResult>()
        val target = buildRequestInto(request, deferred)

        return try {
            deferred.await()
        } finally {
            withContext(Dispatchers.Main) {
                requestManager.clear(target)
            }
        }
    }

    override fun enqueue(request: ImageRequest): ImageRequestDisposable {
        val deferred = CompletableDeferred<ImageResult>()
        val target = buildRequestInto(request, deferred)

        return GlideDisposable(
            deferred = deferred,
            onDispose = {
                Handler(Looper.getMainLooper()).post {
                    requestManager.clear(target)
                }
            }
        )
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
                // preload() into a fixed 1×1 target — result goes to cache only
                .preload(1, 1)
        }

        return deferred.await()
    }

    override fun clearMemoryCache() {
        // Glide requires memory cache clearing on the main thread
        Handler(Looper.getMainLooper()).post {
            Glide.get(context).clearMemory()
        }
    }

    override suspend fun clearDiskCache() {
        // Glide requires disk cache clearing on a background thread
        withContext(Dispatchers.IO) {
            Glide.get(context).clearDiskCache()
        }
    }

    override fun shutdown() {
        requestManager.pauseAllRequests()
    }

    // ── Internal request builder ──────────────────────────────────────────────

    /**
     * Translates an [ImageRequest] into a Glide request and starts it into a
     * [CustomTarget] that writes results back to [deferred].
     *
     * Must be called on the main thread (Glide requirement).
     */
    private fun buildRequestInto(
        request: ImageRequest,
        deferred: CompletableDeferred<ImageResult>,
    ): Target<Drawable> {

        // 1. Instantiation must happen synchronously on the calling thread
        // so we can safely return the target object immediately.
        val target = object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                // Note: If you are using a global RequestListener to complete your deferred,
                // you can keep this blank or manage individual fallback targets here.
                if (!deferred.isCompleted) {
                    deferred.complete(
                        ImageResult.Success(
                            drawable = PlatformImage(painter = AndroidDrawablePainter(resource)),
                            dataSource = DataSource.NETWORK, // Will be refined if using an absolute listener
                            request = request,
                        )
                    )
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // No-op
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                if (!deferred.isCompleted) {
                    deferred.complete(
                        ImageResult.Error(
                            throwable = Exception("Glide load failed for: ${request.data}"),
                            request = request,
                        )
                    )
                }
            }
        }

        // 2. Resolve the data payload model with its integrated headers synchronously
        val modelToLoad: Any = if (request.headers.isNotEmpty()) {
            val headersBuilder = LazyHeaders.Builder()
            request.headers.forEach { (k, v) -> headersBuilder.addHeader(k, v) }
            GlideUrl(request.data.toString(), headersBuilder.build())
        } else {
            request.data // Base data type fallback (String/Uri/File/Int)
        }

        // 3. Dispatch only the side-effect loading action safely to the Android Main Thread
        Handler(Looper.getMainLooper()).post {
            val options = RequestOptions().apply {
                request.size.applyTo(this)
            }

            var requestBuilder = requestManager
                .load(modelToLoad) // Injects data cleanly alongside computed headers
                .apply(options)

            // Setup transitions if requested
            if (request.crossfade) {
                requestBuilder = requestBuilder.transition(
                    DrawableTransitionOptions.withCrossFade(request.crossfadeDurationMs)
                )
            }

            // Attach optional independent tracking listener
            // requestBuilder = requestBuilder.listener(DataSourceListener(deferred, request))

            // Initiate the stream targeting your custom target bridge
            requestBuilder.into(target)
        }

        // 4. Safe Return: This target reference exists now and won't throw an error!
        return target
    }
}

// ── DataSourceListener ────────────────────────────────────────────────────────

/**
 * Intercepts the Glide callback to extract the real [GlideDataSource] and
 * replace the placeholder [DataSource.NETWORK] in the deferred result.
 */
private class DataSourceListener(
    private val deferred: CompletableDeferred<ImageResult>,
    private val request: ImageRequest,
) : RequestListener<Drawable> {

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: GlideDataSource,
        isFirstResource: Boolean,
    ): Boolean {
        // Replace whatever the CustomTarget already completed with the accurate DataSource.
        // CompletableDeferred ignores complete() if already completed, so we cancel +
        // repost only if it hasn't been picked up yet.
        if (!deferred.isCompleted) {
            deferred.complete(
                ImageResult.Success(
                    drawable = PlatformImage(AndroidDrawablePainter(resource)),
                    dataSource = dataSource.toDataSource(),
                    request = request,
                )
            )
        }
        // Return false so Glide still delivers to the CustomTarget
        return false
    }

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean,
    ): Boolean {
        if (!deferred.isCompleted) {
            deferred.complete(
                ImageResult.Error(
                    throwable = e ?: Exception("Glide load failed"),
                    request = request,
                )
            )
        }
        return false
    }
}

// ── GlideDisposable ───────────────────────────────────────────────────────────

private class GlideDisposable(
    private val deferred: CompletableDeferred<ImageResult>,
    private val onDispose: () -> Unit,
) : ImageRequestDisposable {

    @Volatile
    override var isDisposed: Boolean = false
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

// ── Mapping extension functions ───────────────────────────────────────────────

private fun Any.toGlideModel(): Any = when (this) {
    is ImageSource.Url -> this.url
    is ImageSource.Resource -> this.resId
    is ImageSource.LocalFile -> java.io.File(this.path)
    is ImageSource.Bytes -> this.data
    else -> this
}

private fun ImageSource.toDrawableModel(): Any = when (this) {
    is ImageSource.Resource -> resId
    is ImageSource.Url -> url
    is ImageSource.LocalFile -> java.io.File(path)
    is ImageSource.Bytes -> data
}

private fun RequestSize.applyTo(options: RequestOptions) {
    when (this) {
        is RequestSize.Original -> options.override(Target.SIZE_ORIGINAL)
        is RequestSize.Fixed -> options.override(width, height)
    }
}

//private fun RequestPriority.toGlidePriority(): GlidePriority = when (this) {
//    RequestPriority.LOW -> GlidePriority.LOW
//    RequestPriority.NORMAL -> GlidePriority.NORMAL
//    RequestPriority.HIGH -> GlidePriority.HIGH
//}

private fun CachePolicy.applyTo(options: RequestOptions) {
    when (this) {
        CachePolicy.ENABLED -> options.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        CachePolicy.DISABLED -> options
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)

        CachePolicy.READ_ONLY -> options.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .onlyRetrieveFromCache(true)

        CachePolicy.WRITE_ONLY -> options.diskCacheStrategy(DiskCacheStrategy.DATA)
            .skipMemoryCache(false)
    }
}

private fun GlideDataSource.toDataSource(): DataSource = when (this) {
    GlideDataSource.MEMORY_CACHE -> DataSource.MEMORY_CACHE
    GlideDataSource.DATA_DISK_CACHE,
    GlideDataSource.RESOURCE_DISK_CACHE -> DataSource.DISK

    GlideDataSource.REMOTE -> DataSource.NETWORK
    GlideDataSource.LOCAL -> DataSource.DISK
}
