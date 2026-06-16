package com.stockgro.mediapod

/**
 * Where an image source lives.
 *
 * Platform-specific sources (Android Uri, iOS NSData, etc.) should be passed
 * as raw [Any] in [ImageRequest.data]; the platform implementation knows how to
 * handle them natively.
 */
sealed class ImageSource {
    /** A bundled drawable / image resource identified by its integer resource ID. */
    data class Resource(val resId: Int) : ImageSource()

    /** A remote URL (http/https) or a file URL. */
    data class Url(val url: String) : ImageSource()

    /** An absolute path to a file on disk. */
    data class LocalFile(val path: String) : ImageSource()

    /** Raw bytes, e.g. already-downloaded image data. */
    data class Bytes(val data: ByteArray) : ImageSource() {
        override fun equals(other: Any?) = other is Bytes && data.contentEquals(other.data)
        override fun hashCode() = data.contentHashCode()
    }
}