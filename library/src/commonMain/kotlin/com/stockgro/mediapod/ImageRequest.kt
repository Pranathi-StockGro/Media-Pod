package com.stockgro.mediapod

import com.stockgro.mediapod.enums.CachePolicy
import com.stockgro.mediapod.utils.RequestSize

class ImageRequest private constructor(
    /** The source of the image — URL string, ByteArray, platform Uri/File, etc. */
    val data: Any,

    /** The pixel dimensions the loader should decode at. */
    val size: RequestSize,

    /** How the memory caches are consulted for this request. */
    val memoryCachePolicy: CachePolicy,

    /** How the disk caches are consulted for this request. */
    val diskCachePolicy: CachePolicy,

    /** Scheduler hint — higher priority requests jump ahead of lower ones. */
//    val priority: RequestPriority,

    /** Extra HTTP headers forwarded with the network request. */
    val headers: Map<String, String>,

    /** Whether the loader should crossfade from placeholder → result. */
    val crossfade: Boolean,

    /** Crossfade duration in milliseconds. Ignored when [crossfade] is false. */
    val crossfadeDurationMs: Int,
) {

    class Builder(private val data: Any) {
        private var target: ImageTarget? = null

        //        private var placeholder: ImageSource? = null
//        private var error: ImageSource? = null
//        private var fallback: ImageSource? = null
        private var size: RequestSize = RequestSize.Original
        private var memoryCachePolicy: CachePolicy = CachePolicy.ENABLED

        private var diskCachePolicy: CachePolicy = CachePolicy.ENABLED

        //        private var priority: RequestPriority = RequestPriority.NORMAL
        private val headers: MutableMap<String, String> = mutableMapOf()

        //        private var memoryCacheKey: String? = null
        private var crossfade: Boolean = true
        private var crossfadeDurationMs: Int = 300

//        fun placeholder(source: ImageSource) = apply { this.placeholder = source }
//        fun error(source: ImageSource) = apply { this.error = source }
//        fun fallback(source: ImageSource) = apply { this.fallback = source }

        fun size(width: Int, height: Int) = apply {
            this.size = RequestSize.Fixed(width, height)
        }

        fun size(size: RequestSize) = apply { this.size = size }

        fun memoryCachePolicy(policy: CachePolicy) = apply { this.memoryCachePolicy = policy }
        fun diskCachePolicy(policy: CachePolicy) = apply { this.diskCachePolicy = policy }
//        fun priority(priority: RequestPriority) = apply { this.priority = priority }

        fun header(name: String, value: String) = apply { this.headers[name] = value }
        fun headers(map: Map<String, String>) = apply { this.headers += map }

//        fun memoryCacheKey(key: String) = apply { this.memoryCacheKey = key }

        fun crossfade(enabled: Boolean) = apply { this.crossfade = enabled }
        fun crossfade(durationMs: Int) = apply {
            this.crossfade = true
            this.crossfadeDurationMs = durationMs
        }

        fun build() = ImageRequest(
            data = data,
            size = size,
            memoryCachePolicy = memoryCachePolicy,
            diskCachePolicy = diskCachePolicy,
//            priority = priority,
            headers = headers.toMap(),
            crossfade = crossfade,
            crossfadeDurationMs = crossfadeDurationMs,
        )
    }
}

