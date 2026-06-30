package com.stockgro.prefetch.datasource

import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.datasource.SmartMediaLoader
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSObject

/**
 * An iOS-specific [AVAssetResourceLoaderDelegate] implementation.
 *
 * It intercepts iOS AVFoundation media requests and serves them from the local
 * [ChunkMerger] cache, with an optional network fallback using [MediaPrefetchManager].
 *
 * @property chunkMerger The logic for reading from local chunks.
 * @property scope Coroutine scope for background data fetching.
 * @property prefetchManager Manager used for network fallback.
 * @property allowNetworkFallback Whether to attempt network fetching if cache is missing.
 */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class ChunkMergerResourceLoader(
    url: String,
    private val chunkMerger: ChunkMerger,
    private val scope: CoroutineScope,
    private val prefetchManager: MediaPrefetchManager,
    private val allowNetworkFallback: Boolean = true
) : NSObject(), AVAssetResourceLoaderDelegateProtocol {

    private val smartLoader = SmartMediaLoader(url, prefetchManager, chunkMerger, allowNetworkFallback)

    override fun resourceLoader(
        resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource: AVAssetResourceLoadingRequest
    ): Boolean {
        val contentInformationRequest = shouldWaitForLoadingOfRequestedResource.contentInformationRequest
        if (contentInformationRequest != null) {
            val contentType = chunkMerger.contentType ?: "video/mp4"
            contentInformationRequest.contentType = when {
                contentType.contains("mp4") -> "public.mpeg-4"
                contentType.contains("mov") || contentType.contains("quicktime") -> "com.apple.quicktime-movie"
                else -> contentType
            }
            contentInformationRequest.contentLength = chunkMerger.totalSize
            contentInformationRequest.byteRangeAccessSupported = true
            
            // If there's no data request, we can finish here
            if (shouldWaitForLoadingOfRequestedResource.dataRequest == null) {
                shouldWaitForLoadingOfRequestedResource.finishLoading()
                return true
            }
        }

        val dataRequest = shouldWaitForLoadingOfRequestedResource.dataRequest
        if (dataRequest != null) {
            val requestedOffset = dataRequest.requestedOffset
            val requestedLength = dataRequest.requestedLength
            
            scope.launch(Dispatchers.IO) {
                try {
                    val requestedEndOffset = requestedOffset + requestedLength
                    var currentOffset = dataRequest.currentOffset

                    while (currentOffset < requestedEndOffset) {
                        if (shouldWaitForLoadingOfRequestedResource.cancelled || shouldWaitForLoadingOfRequestedResource.finished) {
                            return@launch
                        }

                        val bytesRemainingInRequest = (requestedEndOffset - currentOffset).toInt()
                        val bytesToRead = minOf(bytesRemainingInRequest, 512 * 1024) 
                        val buffer = ByteArray(bytesToRead)

                        val bytesRead = smartLoader.read(
                            position = currentOffset,
                            length = bytesToRead,
                            target = buffer,
                            targetOffset = 0
                        )
                        
                        if (bytesRead > 0) {
                            val data = buffer.usePinned { pinned ->
                                NSData.create(bytes = pinned.addressOf(0), length = bytesRead.toULong())
                            }
                            dataRequest.respondWithData(data)
                            currentOffset += bytesRead
                        } else {
                            // EOF or error
                            break
                        }
                    }
                    
                    if (!shouldWaitForLoadingOfRequestedResource.finished && !shouldWaitForLoadingOfRequestedResource.cancelled) {
                        val reachedEOF = currentOffset >= chunkMerger.totalSize
                        if (dataRequest.currentOffset >= requestedEndOffset || reachedEOF) {
                            shouldWaitForLoadingOfRequestedResource.finishLoading()
                        } else {
                            shouldWaitForLoadingOfRequestedResource.finishLoadingWithError(
                                platform.Foundation.NSError.errorWithDomain("com.stockgro.mediapod", -1001, null)
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (!shouldWaitForLoadingOfRequestedResource.finished && !shouldWaitForLoadingOfRequestedResource.cancelled) {
                        shouldWaitForLoadingOfRequestedResource.finishLoadingWithError(null)
                    }
                }
            }
            return true
        } else if (contentInformationRequest != null) {
            shouldWaitForLoadingOfRequestedResource.finishLoading()
            return true
        }

        return false
    }
}
