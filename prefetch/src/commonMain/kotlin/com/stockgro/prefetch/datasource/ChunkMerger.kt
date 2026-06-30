package com.stockgro.prefetch.datasource

import com.stockgro.prefetch.data.PrefetchChunkEntity
import com.stockgro.prefetch.util.throwIfCancelled
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.min

/**
 * Orchestrates the reading of media data from multiple cached chunks.
 *
 * It acts as a virtual stream, seamlessly stitching together local files to provide
 * a continuous byte range to the media player.
 *
 * @property totalSize Total size of the media file in bytes.
 * @property contentType The MIME type of the media.
 * @property chunkSize The size of each chunk used for storage.
 * @property getChunk A lambda that provides a [PrefetchChunkEntity] for a given chunk index.
 */
class ChunkMerger(
    val totalSize: Long,
    val contentType: String?,
    val chunkSize: Int,
    private val getChunk: suspend (Int) -> PrefetchChunkEntity?
) {
    private val chunkCache = mutableMapOf<Int, PrefetchChunkEntity>()
    private val cacheMutex = Mutex()

    // Optimization: Memory buffer for small reads and to avoid repetitive file opening
    private var bufferChunkIndex: Int = -1
    private var bufferStartPosition: Long = -1L
    private var bufferData: ByteArray? = null
    private val internalBufferSize = 256 * 1024 // 256KB buffer

    /**
     * Reads up to [length] bytes into [target] starting at [targetOffset].
     * Seeks across chunks as needed to fulfill the request.
     *
     * @param position The absolute byte position in the media file to start reading from.
     * @param length The maximum number of bytes to read.
     * @param target The byte array to write data into.
     * @param targetOffset The offset in [target] to start writing.
     * @return The number of bytes read, or -1 if the end of the stream is reached.
     */
    suspend fun read(position: Long, length: Int, target: ByteArray, targetOffset: Int): Int {
        if (position >= totalSize) return -1

        val bytesToRead = min(length.toLong(), totalSize - position).toInt()
        var currentPos = position
        var totalRead = 0

        while (totalRead < bytesToRead) {
            val chunkIndex = (currentPos / chunkSize).toInt()
            
            // 1. Try reading from memory buffer first
            val readFromBuf = readFromMemoryBuffer(currentPos, bytesToRead - totalRead, target, targetOffset + totalRead)
            if (readFromBuf > 0) {
                totalRead += readFromBuf
                currentPos += readFromBuf
                continue
            }

            // 2. Buffer miss or empty, try filling buffer from file
            val chunk = getChunkCached(chunkIndex) ?: break
            
            if (fillMemoryBuffer(currentPos, chunk)) {
                val readAfterFill = readFromMemoryBuffer(currentPos, bytesToRead - totalRead, target, targetOffset + totalRead)
                if (readAfterFill > 0) {
                    totalRead += readAfterFill
                    currentPos += readAfterFill
                    continue
                }
            }

            // 3. If buffer logic failed for some reason, fall back to direct file read
            val fileOffset = currentPos % chunkSize
            val bytesAvailableInChunk = (chunk.endByte - currentPos + 1)
            val chunkReadLimit = min(bytesAvailableInChunk, (bytesToRead - totalRead).toLong()).toInt()

            if (chunkReadLimit <= 0) break

            val readFromChunk = readFromFile(
                Path(chunk.localFilePath),
                fileOffset,
                chunkReadLimit,
                target,
                targetOffset + totalRead
            )

            if (readFromChunk <= 0) break

            totalRead += readFromChunk
            currentPos += readFromChunk
        }

        if (totalRead == 0 && position < totalSize) {
            // Attempt to read just one more byte if we made no progress but aren't at the end
            val chunkIndex = (position / chunkSize).toInt()
            val chunk = getChunkCached(chunkIndex) ?: return 0

            val fileOffset = position % chunkSize
            val read = readFromFile(
                Path(chunk.localFilePath),
                fileOffset,
                1,
                target,
                targetOffset
            )
            return if (read <= 0) 0 else read
        }
        return totalRead
    }

    private suspend fun getChunkCached(index: Int): PrefetchChunkEntity? {
        return cacheMutex.withLock {
            chunkCache[index] ?: getChunk(index)?.also { chunkCache[index] = it }
        }
    }

    private fun readFromMemoryBuffer(position: Long, length: Int, target: ByteArray, targetOffset: Int): Int {
        val data = bufferData ?: return 0
        if (position < bufferStartPosition || position >= bufferStartPosition + data.size) return 0

        val offsetInBuffer = (position - bufferStartPosition).toInt()
        val available = data.size - offsetInBuffer
        val toRead = minOf(length, available)

        data.copyInto(target, targetOffset, offsetInBuffer, offsetInBuffer + toRead)
        return toRead
    }

    private fun fillMemoryBuffer(position: Long, chunk: PrefetchChunkEntity): Boolean {
        val chunkIndex = (position / chunkSize).toInt()
        val fileOffset = position % chunkSize
        
        // We want to buffer from 'position' up to 'internalBufferSize'
        // But not crossing chunk boundaries
        val bytesAvailableInChunk = (chunk.endByte - position + 1)
        val toBuffer = minOf(internalBufferSize.toLong(), bytesAvailableInChunk).toInt()
        
        if (toBuffer <= 0) return false

        val newData = ByteArray(toBuffer)
        val read = readFromFile(Path(chunk.localFilePath), fileOffset, toBuffer, newData, 0)
        
        if (read > 0) {
            bufferChunkIndex = chunkIndex
            bufferStartPosition = position
            bufferData = if (read < toBuffer) newData.copyOf(read) else newData
            return true
        }
        return false
    }

    private fun readFromFile(
        path: Path,
        offset: Long,
        length: Int,
        target: ByteArray,
        targetOffset: Int
    ): Int {
        if (!SystemFileSystem.exists(path)) return 0

        return runCatching {
            SystemFileSystem.source(path).buffered().use { source ->
                if (offset > 0) {
                    source.skip(offset)
                }
                source.readAtMostTo(target, targetOffset, length)
            }
        }.throwIfCancelled().getOrElse { 0 }
    }
}
