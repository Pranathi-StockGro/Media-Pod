package com.stockgro.prefetch.datasource

import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.util.throwIfCancelled

/**
 * A platform-agnostic orchestrator for media data retrieval.
 *
 * It combines local cache reading (via [ChunkMerger]) with proactive background prefetching
 * and on-demand network fallbacks. This ensures smooth playback while actively
 * populating the persistent cache.
 */
class SmartMediaLoader(
    private val url: String,
    private val prefetchManager: MediaPrefetchManager,
    private val chunkMerger: ChunkMerger,
    private val allowNetworkFallback: Boolean = true
) {
    private var lastPrefetchedIndex = -1
    private var lastPrefetchedNextIndex = -1

    /**
     * Reads media data into the provided [target] array.
     *
     * @param position The absolute byte position in the media file.
     * @param length The number of bytes to read.
     * @param target The destination byte array.
     * @param targetOffset The starting offset in [target].
     * @param finalFallback Optional lambda for platform-specific final network fallback (e.g. ExoPlayer's upstream).
     * @return The number of bytes read, or -1 for EOF, or 0 if no data could be retrieved.
     */
    suspend fun read(
        position: Long,
        length: Int,
        target: ByteArray,
        targetOffset: Int,
        finalFallback: (suspend (pos: Long, len: Int) -> Int)? = null
    ): Int {
        if (length == 0) return 0
        
        val currentChunkIndex = (position / chunkMerger.chunkSize).toInt()
        val positionInChunk = position % chunkMerger.chunkSize

        // 1. Proactive prefetch: trigger background download for the next chunk as we approach its boundary.
        if (positionInChunk > chunkMerger.chunkSize * 0.75) {
            val nextIndex = currentChunkIndex + 1
            if (nextIndex != lastPrefetchedNextIndex) {
                lastPrefetchedNextIndex = nextIndex
                prefetchManager.prefetchChunk(url, nextIndex)
            }
        }

        // 2. Try reading from the local cache (SSOT: Room DB + Filesystem).
        val bytesRead = runCatching {
            chunkMerger.read(position, length, target, targetOffset)
        }.throwIfCancelled().getOrElse { 0 }

        if (bytesRead > 0) return bytesRead

        // 3. Cache Miss: The required data is not in the local database.
        if (allowNetworkFallback) {
            // Trigger a full background chunk download to eventually fill the SSOT.
            if (currentChunkIndex != lastPrefetchedIndex) {
                lastPrefetchedIndex = currentChunkIndex
                prefetchManager.prefetchChunk(url, currentChunkIndex)
                prefetchManager.prefetchChunk(url, currentChunkIndex + 1)
            }

            // 4. Fast Fallback: Satisfy the player's immediate need with a small network request.
            val networkData = prefetchManager.fetchRange(url, position, length)
            if (networkData != null && networkData.isNotEmpty()) {
                val actualRead = minOf(networkData.size, length)
                networkData.copyInto(target, targetOffset, 0, actualRead)
                return actualRead
            }
        }

        // 5. Final Fallback: Use platform-specific loader if all else fails.
        return finalFallback?.invoke(position, length) ?: (if (bytesRead == -1) -1 else 0)
    }
}
