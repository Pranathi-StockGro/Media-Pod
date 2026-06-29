package com.stockgro.prefetch.datasource

import com.stockgro.prefetch.data.PrefetchChunkEntity
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.min

class ChunkMerger(
    val totalSize: Long,
    val contentType: String?,
    private val chunkSize: Int,
    private val getChunk: suspend (Int) -> PrefetchChunkEntity?
) {
    /**
     * Reads up to [length] bytes into [target] starting at [targetOffset].
     * Returns the number of bytes read, or -1 if the end of the stream is reached.
     */
    suspend fun read(position: Long, length: Int, target: ByteArray, targetOffset: Int): Int {
        if (position >= totalSize) return -1

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

        return if (totalRead == 0 && position < totalSize) 0 else totalRead
    }

    private fun readFromFile(
        path: Path,
        offset: Long,
        length: Int,
        target: ByteArray,
        targetOffset: Int
    ): Int {
        if (!SystemFileSystem.exists(path)) return 0

        return try {
            SystemFileSystem.source(path).buffered().use { source ->
                if (offset > 0) {
                    source.skip(offset)
                }
                source.readAtMostTo(target, targetOffset, targetOffset + length)
            }
        } catch (e: Exception) {
            0
        }
    }
}
