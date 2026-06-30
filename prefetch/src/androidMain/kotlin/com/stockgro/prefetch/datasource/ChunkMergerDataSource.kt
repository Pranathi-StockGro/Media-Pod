package com.stockgro.prefetch.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.util.throwIfCancelled
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * An Android-specific [DataSource] implementation for Media3/ExoPlayer.
 *
 * It attempts to read data from the local [ChunkMerger] cache first. If chunks are missing,
 * it fetches them via [prefetchManager], ensuring they are saved to the Room DB
 * for the Single Source of Truth requirement.
 *
 * @property url The media URL.
 * @property prefetchManager The manager for on-demand fetching and caching.
 * @property chunkMerger The logic for reading from local chunks.
 * @property upstreamDataSource The fallback network data source.
 */
@UnstableApi
class ChunkMergerDataSource(
    private val url: String,
    private val prefetchManager: MediaPrefetchManager,
    private val chunkMerger: ChunkMerger,
    private val upstreamDataSource: DataSource? = null
) : BaseDataSource(false) {

    private var dataSpec: DataSpec? = null
    private var opened = false
    private var currentPosition = 0L
    private var bytesRemaining = 0L
    private var openedUpstream = false
    private var lastPrefetchedIndex = -1
    private var lastPrefetchedNextIndex = -1
    private val lock = Any()

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.currentPosition = dataSpec.position
        this.lastPrefetchedIndex = -1
        this.lastPrefetchedNextIndex = -1

        synchronized(lock) {
            if (openedUpstream) {
                try {
                    upstreamDataSource?.close()
                } catch (e: IOException) {
                    // Ignore failure to close previous upstream
                }
            }
            openedUpstream = false
        }

        transferInitializing(dataSpec)

        val totalSize = chunkMerger.totalSize

        val length = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            totalSize - dataSpec.position
        }

        if (length < 0) {
            throw IOException("Invalid data range: position ${dataSpec.position}, total size $totalSize")
        }

        this.bytesRemaining = length
        this.opened = true
        transferStarted(dataSpec)
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()

        val currentChunkIndex = (currentPosition / chunkMerger.chunkSize).toInt()
        val positionInChunk = currentPosition % chunkMerger.chunkSize

        if (positionInChunk > chunkMerger.chunkSize * 0.75) {
            val nextIndex = currentChunkIndex + 1
            if (nextIndex != lastPrefetchedNextIndex) {
                lastPrefetchedNextIndex = nextIndex
                prefetchManager.prefetchChunk(url, nextIndex)
            }
        }

        // 1. Try reading from the cache (stitching chunks already in DB)
        val bytesRead = runCatching {
            runBlocking {
                chunkMerger.read(currentPosition, bytesToRead, buffer, offset)
            }
        }.throwIfCancelled().getOrElse { 0 }

        if (bytesRead > 0) {
            currentPosition += bytesRead
            bytesRemaining -= bytesRead
            bytesTransferred(bytesRead)
            return bytesRead
        }

        // 2. Cache Miss: We hit a boundary where data isn't in the DB yet.
        // Trigger a background prefetch of the FULL chunk to populate the DB.
        val chunkIndex = (currentPosition / chunkMerger.chunkSize).toInt()
        
        if (chunkIndex != lastPrefetchedIndex) {
            lastPrefetchedIndex = chunkIndex
            prefetchManager.prefetchChunk(url, chunkIndex)
            // Also trigger prefetch for the NEXT chunk to avoid the stutter at the next boundary
            prefetchManager.prefetchChunk(url, chunkIndex + 1)
        }

        // 3. Fallback: Satisfy the immediate player request with a small network fetch.
        // This is fast and prevents the 2-second stutter caused by full-chunk downloads.
        val networkData = runCatching {
            runBlocking {
                prefetchManager.fetchRange(url, currentPosition, bytesToRead)
            }
        }.throwIfCancelled().getOrNull()

        if (networkData != null && networkData.isNotEmpty()) {
            val actualRead = minOf(networkData.size, length)
            networkData.copyInto(buffer, offset, 0, actualRead)
            currentPosition += actualRead
            bytesRemaining -= actualRead
            bytesTransferred(actualRead)
            return actualRead
        }

        // 4. Final fallback to raw upstream if available
        return readFromNetwork(buffer, offset, length)
    }

    private fun readFromNetwork(buffer: ByteArray, offset: Int, length: Int): Int {
        val upstream = upstreamDataSource ?: return C.RESULT_END_OF_INPUT
        val currentDataSpec = dataSpec ?: return C.RESULT_END_OF_INPUT

        var needsOpen = false
        var subrangeDataSpec: DataSpec? = null

        synchronized(lock) {
            if (!openedUpstream) {
                needsOpen = true
                subrangeDataSpec = currentDataSpec.subrange(currentPosition - currentDataSpec.position)
            }
        }

        if (needsOpen) {
            runCatching {
                upstream.open(subrangeDataSpec!!)
            }.throwIfCancelled().onSuccess {
                synchronized(lock) {
                    openedUpstream = true
                }
            }.onFailure { e ->
                synchronized(lock) {
                    openedUpstream = false
                }
                try {
                    upstream.close()
                } catch (closeException: Exception) {
                    // Ignore close failure after open failure
                }
                throw e
            }
        }

        val bytesRead = upstream.read(buffer, offset, length)
        if (bytesRead != C.RESULT_END_OF_INPUT) {
            currentPosition += bytesRead
            bytesRemaining -= bytesRead
            bytesTransferred(bytesRead)
        }
        return bytesRead
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        if (opened) {
            opened = false
            try {
                upstreamDataSource?.close()
            } catch (e: IOException) {
                // Ignore failure to close upstream
            } finally {
                synchronized(lock) {
                    openedUpstream = false
                }
            }
            transferEnded()
        }
        dataSpec = null
    }
}

/**
 * Factory for creating [ChunkMergerDataSource] instances.
 *
 * @property prefetchManager The manager for on-demand fetching and caching.
 * @property upstreamFactory Factory for the fallback network data source.
 * @property chunkMergerProvider Lambda that provides a [ChunkMerger] for a given media identifier.
 */
class ChunkMergerDataSourceFactory(
    private val url: String,
    private val prefetchManager: MediaPrefetchManager,
    private val upstreamFactory: DataSource.Factory? = null,
    private val chunkMergerProvider: (String) -> ChunkMerger
) : DataSource.Factory {

    @UnstableApi
    override fun createDataSource(): DataSource {
        val chunkMerger = chunkMergerProvider(url)
        return ChunkMergerDataSource(url, prefetchManager, chunkMerger, upstreamFactory?.createDataSource())
    }
}
