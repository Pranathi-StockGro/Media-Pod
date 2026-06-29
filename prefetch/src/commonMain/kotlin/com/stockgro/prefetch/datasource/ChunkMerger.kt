package com.stockgro.prefetch.datasource

import com.stockgro.prefetch.data.PrefetchChunkEntity
import com.stockgro.prefetch.util.throwIfCancelled
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
    private val chunkSize: Int,
    private val getChunk: suspend (Int) -> PrefetchChunkEntity?
) {
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
    suspend fun read(position: Long, length: Int, target: ByteArray, targetOffset: Int): Int =
        withContext(kotlinx.coroutines.Dispatchers.Default) {
            if (position >= totalSize) return@withContext -1

            val bytesToRead = min(length.toLong(), totalSize - position).toInt()
            var currentPos = position
            var totalRead = 0

            while (totalRead < bytesToRead) {
                val chunkIndex = (currentPos / chunkSize).toInt()
                val chunk = getChunk(chunkIndex) ?: break

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
                val chunk = getChunk(chunkIndex) ?: return@withContext 0

                val fileOffset = position % chunkSize
                val read = readFromFile(
                    Path(chunk.localFilePath),
                    fileOffset,
                    1,
                    target,
                    targetOffset
                )
                return@withContext if (read <= 0) 0 else read
            }
            totalRead
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
