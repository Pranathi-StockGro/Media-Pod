package com.stockgro.prefetch.datasource

import com.stockgro.prefetch.MediaPrefetchManager
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
 * @property originalUrl The original media URL for fallback requests.
 * @property allowNetworkFallback Whether to attempt network fetching if cache is missing.
 */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class ChunkMergerResourceLoader(
    private val chunkMerger: ChunkMerger,
    private val scope: CoroutineScope,
    private val prefetchManager: MediaPrefetchManager? = null,
    private val originalUrl: String? = null,
    private val allowNetworkFallback: Boolean = true
) : NSObject(), AVAssetResourceLoaderDelegateProtocol {

    private var lastPrefetchedIndex = -1
    private var lastPrefetchedNextIndex = -1

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
                        
                        // Proactive prefetch: check if we are getting close to the next chunk boundary
                        val currentChunkIndex = (currentOffset / chunkMerger.chunkSize).toInt()
                        val positionInChunk = currentOffset % chunkMerger.chunkSize
                        
                        if (positionInChunk > chunkMerger.chunkSize * 0.75) {
                            val nextIndex = currentChunkIndex + 1
                            if (nextIndex != lastPrefetchedNextIndex && prefetchManager != null && originalUrl != null) {
                                lastPrefetchedNextIndex = nextIndex
                                prefetchManager.prefetchChunk(originalUrl, nextIndex)
                            }
                        }

                        val buffer = ByteArray(bytesToRead)

                        val bytesRead = readFromCache(currentOffset, bytesToRead, buffer)
                        
                        if (bytesRead > 0) {
                            val data = buffer.usePinned { pinned ->
                                NSData.create(bytes = pinned.addressOf(0), length = bytesRead.toULong())
                            }
                            dataRequest.respondWithData(data)
                            currentOffset += bytesRead
                        } else if (bytesRead == 0 && allowNetworkFallback) {
                            // Cache Miss: We hit a boundary where data isn't in the DB yet.
                            if (currentChunkIndex != lastPrefetchedIndex && prefetchManager != null && originalUrl != null) {
                                lastPrefetchedIndex = currentChunkIndex
                                prefetchManager.prefetchChunk(originalUrl, currentChunkIndex)
                                prefetchManager.prefetchChunk(originalUrl, currentChunkIndex + 1)
                            }

                            val networkData = readFromNetwork(currentOffset, bytesToRead)
                            if (networkData != null && networkData.isNotEmpty()) {
                                val data = networkData.usePinned { pinned ->
                                    NSData.create(bytes = pinned.addressOf(0), length = networkData.size.toULong())
                                }
                                dataRequest.respondWithData(data)
                                currentOffset += networkData.size
                            } else {
                                // Network also failed or returned no data
                                break
                            }
                        } else {
                            // EOF or error with no fallback
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

    private suspend fun readFromCache(position: Long, length: Int, buffer: ByteArray): Int {
        return chunkMerger.read(position, length, buffer, 0)
    }

    private suspend fun readFromNetwork(position: Long, length: Int): ByteArray? {
        val manager = prefetchManager ?: return null
        val url = originalUrl ?: return null
        return manager.fetchRange(url, position, length)
    }
}
