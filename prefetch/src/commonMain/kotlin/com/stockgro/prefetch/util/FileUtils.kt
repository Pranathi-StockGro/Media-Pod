package com.stockgro.prefetch.util

import com.stockgro.prefetch.exception.FileOperationException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readFully
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.min

/**
 * Utility functions for filesystem and network stream operations.
 * Handles parent directory creation, atomic moves, and safe channel-to-file saving.
 */
object FileUtils {

    private val fileNameSanitizer = Regex("[^a-zA-Z0-9.\\-_]")
    fun Path.ensureParentExists() {
        parent?.let {
            if (!SystemFileSystem.exists(it)) {
                SystemFileSystem.createDirectories(it)
            }
        }
    }

    fun Path.ensureExists() {
        if (!SystemFileSystem.exists(this)) {
            SystemFileSystem.createDirectories(this)
        }
    }

    /**
     * Reads from a [ByteReadChannel] and saves the content to a file.
     * Deletes the partial file if the operation fails.
     *
     * @param channel The input stream to read from.
     * @param path The destination file path.
     * @param expectedLength Total number of bytes to read from the channel.
     * @param bufferSize Size of the intermediate buffer for copying.
     * @throws FileOperationException if the operation fails.
     */
    suspend fun saveChannelToFile(
        channel: ByteReadChannel,
        path: Path,
        expectedLength: Long,
        bufferSize: Int
    ) {
        runCatching {
            path.ensureParentExists()
            val buffer = ByteArray(bufferSize)
            var totalRead = 0L

            SystemFileSystem.sink(path).buffered().use { sink ->
                while (totalRead < expectedLength) {
                    val toRead = min(buffer.size.toLong(), expectedLength - totalRead).toInt()
                    channel.readFully(buffer, 0, toRead)
                    sink.write(buffer, 0, toRead)
                    totalRead += toRead
                }
                sink.flush()
            }
        }.throwIfCancelled().onFailure { e ->
            if (SystemFileSystem.exists(path)) {
                runCatching {
                    SystemFileSystem.delete(path)
                }
            }
            throw FileOperationException("Failed to save channel to $path", e)
        }
    }

    /**
     * Writes a [ByteArray] to a file.
     * Deletes the partial file if the operation fails.
     *
     * @param data The bytes to write.
     * @param path The destination file path.
     * @throws FileOperationException if the operation fails.
     */
    fun saveByteArrayToFile(
        data: ByteArray,
        path: Path
    ) {
        runCatching {
            path.ensureParentExists()
            SystemFileSystem.sink(path).buffered().use { sink ->
                sink.write(data)
                sink.flush()
            }
        }.throwIfCancelled().onFailure { e ->
            if (SystemFileSystem.exists(path)) {
                runCatching {
                    SystemFileSystem.delete(path)
                }
            }
            throw FileOperationException("Failed to save bytes to $path", e)
        }
    }

    fun atomicMove(source: Path, destination: Path) {
        runCatching {
            if (SystemFileSystem.exists(destination)) {
                SystemFileSystem.delete(destination)
            }
            SystemFileSystem.atomicMove(source, destination)
        }.onFailure { e ->
            throw FileOperationException("Failed to move $source to $destination", e)
        }
    }

    fun delete(path: Path) {
        runCatching {
            if (SystemFileSystem.exists(path)) {
                SystemFileSystem.delete(path)
            }
        }.onFailure { e ->
            throw FileOperationException("Failed to delete $path", e)
        }
    }

    fun sanitizeUrlToFilename(url: String): String {
        return url.replace(fileNameSanitizer, "_")
            .take(120)
    }
}
