package com.stockgro.prefetch.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.stockgro.prefetch.util.throwIfCancelled
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * An Android-specific [DataSource] implementation for Media3/ExoPlayer.
 *
 * It attempts to read data from the local [ChunkMerger] cache first, and falls back
 * to the [upstreamDataSource] (network) if the required chunks are missing.
 *
 * @property chunkMerger The logic for reading from local chunks.
 * @property upstreamDataSource The fallback network data source.
 */
@UnstableApi
class ChunkMergerDataSource(
    private val chunkMerger: ChunkMerger,
    private val upstreamDataSource: DataSource? = null
) : BaseDataSource(false) {

    private var dataSpec: DataSpec? = null
    private var opened = false
    private var currentPosition = 0L
    private var bytesRemaining = 0L
    private var isUsingUpstream = false
    private var openedUpstream = false
    private val lock = Any()

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.currentPosition = dataSpec.position

        synchronized(lock) {
            if (openedUpstream) {
                try {
                    upstreamDataSource?.close()
                } catch (e: IOException) {
                    // Ignore failure to close previous upstream
                }
            }
            isUsingUpstream = false
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

        if (isUsingUpstream) {
            return readFromNetwork(buffer, offset, length)
        }

        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()

        val bytesRead = runCatching {
            runBlocking {
                chunkMerger.read(currentPosition, bytesToRead, buffer, offset)
            }
        }.throwIfCancelled().getOrElse {
            throw IOException("Failed to read from ChunkMerger", it)
        }

        if (bytesRead == -1) {
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRead == 0) {
            if (upstreamDataSource != null) {
                synchronized(lock) {
                    isUsingUpstream = true
                }
                return readFromNetwork(buffer, offset, length)
            }
            throw IOException("Failed to read data at position $currentPosition. Chunk might be missing or download failed.")
        }

        currentPosition += bytesRead
        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
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
                    isUsingUpstream = false
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
 * @property upstreamFactory Factory for the fallback network data source.
 * @property chunkMergerProvider Lambda that provides a [ChunkMerger] for a given media identifier.
 */
class ChunkMergerDataSourceFactory(
    private val upstreamFactory: DataSource.Factory? = null,
    private val chunkMergerProvider: (String) -> ChunkMerger
) : DataSource.Factory {

    @UnstableApi
    override fun createDataSource(): DataSource {
        val chunkMerger = chunkMergerProvider("todo")
        return ChunkMergerDataSource(chunkMerger, upstreamFactory?.createDataSource())
    }
}
