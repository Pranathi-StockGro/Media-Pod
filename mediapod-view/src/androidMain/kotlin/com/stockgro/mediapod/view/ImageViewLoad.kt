package com.stockgro.mediapod.view

import android.view.ViewTreeObserver
import android.widget.ImageView
import com.stockgro.mediapod.ImageLoader
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.ImageRequestDisposable
import com.stockgro.mediapod.ImageResult
import com.stockgro.mediapod.utils.RequestSize
import kotlin.coroutines.cancellation.CancellationException


fun ImageView.load(
    data: Any?,
    imageLoader: ImageLoader = ImageLoaderProvider.default,
    builder: ImageRequest.Builder.() -> Unit = {},
): ImageRequestDisposable {
    fun dispatch(w: Int, h: Int): ImageRequestDisposable {
        val request = ImageRequest.Builder(data ?: "")
            .imageTarget(ImageViewTarget(this))
            .apply { if (w > 0 && h > 0) size(RequestSize.Fixed(w, h)) }
            .apply(builder)
            .build()
        return imageLoader.enqueue(request)
    }

    if (width > 0 && height > 0) {
        return dispatch(width, height)
    }

    val lp = layoutParams
    if (lp != null && lp.width > 0 && lp.height > 0) {
        return dispatch(lp.width, lp.height)
    }

    val pending = PendingDisposable()
    val listener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            if (!pending.isDisposed) pending.delegate = dispatch(width, height)
            return true
        }
    }
    viewTreeObserver.addOnPreDrawListener(listener)
    pending.onCancel = { viewTreeObserver.removeOnPreDrawListener(listener) }
    return pending
}

private class PendingDisposable : ImageRequestDisposable {
    @Volatile
    var delegate: ImageRequestDisposable? = null
    var onCancel: (() -> Unit)? = null

    @Volatile
    override var isDisposed = false; private set
    override fun dispose() {
        isDisposed = true; onCancel?.invoke(); delegate?.dispose()
    }

    override suspend fun await() = delegate?.await()
        ?: ImageResult.Error(CancellationException("disposed before dispatch"), ImageRequest.Builder("").build())
}