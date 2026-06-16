package com.stockgro.mediapod.view

import android.widget.ImageView
import com.stockgro.mediapod.ImageTarget
import com.stockgro.mediapod.PlatformImage

class ImageViewTarget(private val imageView: ImageView) : ImageTarget {

    override fun onStart(placeholder: PlatformImage?) {
        placeholder?.nativeDrawable?.let {
            imageView.setImageDrawable(it)
        }
    }

    override fun onError(error: PlatformImage?) {
        error?.nativeDrawable?.let {
            imageView.setImageDrawable(it)
        }
    }

    override fun onSuccess(result: PlatformImage) {
        result.nativeDrawable?.let {
            imageView.setImageDrawable(it)
        }
    }
}