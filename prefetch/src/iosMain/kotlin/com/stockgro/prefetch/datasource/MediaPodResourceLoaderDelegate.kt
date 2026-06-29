package com.stockgro.prefetch.datasource

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

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class MediaPodResourceLoaderDelegate(
    private val chunkMerger: ChunkMerger,
    private val scope: CoroutineScope
) : NSObject(), AVAssetResourceLoaderDelegateProtocol {

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
        }

        val dataRequest = shouldWaitForLoadingOfRequestedResource.dataRequest
        if (dataRequest != null) {
            val requestedOffset = dataRequest.requestedOffset
            val requestedLength = dataRequest.requestedLength
            val startOffset = dataRequest.currentOffset

            scope.launch(Dispatchers.IO) {
                try {
                    val bytesToRead = (requestedOffset + requestedLength - startOffset).toInt()
                    if (bytesToRead > 0) {
                        val buffer = ByteArray(bytesToRead)
                        val bytesRead = chunkMerger.read(startOffset, bytesToRead, buffer, 0)
                        
                        if (bytesRead > 0) {
                            val data = buffer.usePinned { pinned ->
                                NSData.create(bytes = pinned.addressOf(0), length = bytesRead.toULong())
                            }
                            dataRequest.respondWithData(data)
                        }
                    }
                    if (!shouldWaitForLoadingOfRequestedResource.finished) {
                        shouldWaitForLoadingOfRequestedResource.finishLoading()
                    }
                } catch (e: Exception) {
                    if (!shouldWaitForLoadingOfRequestedResource.finished) {
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
