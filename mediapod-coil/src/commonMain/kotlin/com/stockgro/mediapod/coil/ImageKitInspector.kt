package com.stockgro.mediapod.coil

import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import com.stockgro.mediapod.utils.ImageKitUrlTransformer

class ImageKitInterceptor(private val dpr: Float) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data

        if (data is String && data.contains("cdn.stockgro.com")) {
            val widthPx = resolveDimensionInPixels(chain.size.width)
            val heightPx = resolveDimensionInPixels(chain.size.height)

            // Direct mapping to the primitive shared helper logic
            val modifiedUrl = ImageKitUrlTransformer.transform(
                url = data,
                widthPx = widthPx,
                heightPx = heightPx,
                deviceDpr = dpr
            )

            val newRequest = request.newBuilder()
                .data(modifiedUrl)
                .build()

            val newChain = chain.withRequest(newRequest)
            val result = newChain.proceed()

//            logResponseMetrics(newChain, result, modifiedUrl)
            return result
        }

        return chain.proceed()
    }

    private fun logResponseMetrics(chain: Interceptor.Chain, result: ImageResult, url: String) {
        if (result is SuccessResult) {
            // 1. Calculate RAM footprint (Uncompressed size in memory)
            val ramSizeKb = result.image.size / 1024.0

            // 2. Fetch the network download size using Coil's Singleton instance
            var networkSizeKb = 0.0

            // Extract the diskCacheKey exactly where you see it
            val cacheKey = result.diskCacheKey ?: chain.request.diskCacheKey

            // ✅ FIX FOR COIL 3 KMP: Safely fetch the current active disk cache instance
            val context = chain.request.context
            val diskCache = SingletonImageLoader.get(context).diskCache

            if (diskCache != null && cacheKey != null) {
                diskCache.openSnapshot(cacheKey)?.use { snapshot ->
                    val fileSystem = diskCache.fileSystem
                    val dataFilePath = snapshot.data

                    // Extract the primitive file metric via Okio
                    val fileSizeBytes = fileSystem.metadataOrNull(dataFilePath)?.size ?: 0L
                    networkSizeKb = fileSizeBytes / 1024.0
                }
            }

            println("➔ [ImageKit Engine] ---------------------------------------------")
            println("  URL: $url")
            println("  Source: ${result.dataSource}")
            println("  Network Download File Size : ${networkSizeKb} KB")
            println("  Device RAM Usage Size      : ${ramSizeKb} KB")
            println("➔ ---------------------------------------------------------------")
        } else {
            println("➔ [ImageKit Engine] Network fetch failed for: $url")
        }
    }

    private fun resolveDimensionInPixels(size: Dimension): Int? {
        return when (size) {
            is Dimension.Pixels -> size.px
            else -> null
        }
    }

}