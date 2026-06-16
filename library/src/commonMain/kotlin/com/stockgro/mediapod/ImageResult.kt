package com.stockgro.mediapod

import androidx.compose.ui.graphics.painter.Painter
import com.stockgro.mediapod.enums.DataSource

sealed class ImageResult {

    /**
     * The image loaded successfully.
     *
     * @param drawable   Platform-specific drawable handle wrapped in [PlatformImage].
     * @param dataSource Where the data was read from.
     * @param request    The original request that produced this result.
     */
    data class Success(
        val drawable: PlatformImage,
        val dataSource: DataSource,
        val request: ImageRequest,
    ) : ImageResult()

    /**
     * The load failed.
     *
     * @param throwable  The underlying cause.
     * @param request    The original request.
     */
    data class Error(
        val throwable: Throwable,
        val request: ImageRequest,
    ) : ImageResult()
}

/** Returns true only for [ImageResult.Success]. */
val ImageResult.isSuccess get() = this is ImageResult.Success

/** Returns the [ImageResult.Success.drawable] or null. */
val ImageResult.drawableOrNull get() = (this as? ImageResult.Success)?.drawable


/**
 * Receives image-load lifecycle callbacks.
 *
 * Implement this when you need fine-grained control over how a loaded image
 * is applied. For the common case of loading into an ImageView, use the
 * `ImageView.load(...)` extension instead.
 */
interface ImageTarget {
    /** Called immediately on the main thread with the placeholder (maybe null). */
    fun onStart(placeholder: PlatformImage?) {}

    /** Called on the main thread when the image loaded successfully. */
    fun onSuccess(result: PlatformImage)

    /** Called on the main thread when the load fails. [error] may be null. */
    fun onError(error: PlatformImage?) {}
}

/**
 * A handle returned by [ImageLoader.enqueue].
 *
 * Use [dispose] to cancel the in-flight request; use [await] to
 * suspend until the result is ready.
 */
interface ImageRequestDisposable {
    /** True once the request has been cancelled or has finished. */
    val isDisposed: Boolean

    /** Cancel the request. Safe to call multiple times. */
    fun dispose()

    /** Suspend until the request completes and return its [ImageResult]. */
    suspend fun await(): ImageResult
}

/** Platform-native bitmap type. Resolved per-platform via expect/actual. */
expect class PlatformBitmap

class PlatformImage(val painter: Painter)
