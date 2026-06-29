package com.stockgro.prefetch.util

import com.stockgro.prefetch.exception.FileOperationException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.min

object FileUtils {
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

    suspend fun saveChannelToFile(
        channel: ByteReadChannel,
        path: Path,
        expectedLength: Long,
        bufferSize: Int
    ) {
        try {
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
        } catch (e: Exception) {
            if (SystemFileSystem.exists(path)) {
                try {
                    SystemFileSystem.delete(path)
                } catch (_: Exception) {
                }
            }
            throw FileOperationException("Failed to save channel to $path", e)
        }
    }

    fun atomicMove(source: Path, destination: Path) {
        try {
            if (SystemFileSystem.exists(destination)) {
                SystemFileSystem.delete(destination)
            }
            SystemFileSystem.atomicMove(source, destination)
        } catch (e: Exception) {
            throw FileOperationException("Failed to move $source to $destination", e)
        }
    }

    fun delete(path: Path) {
        try {
            if (SystemFileSystem.exists(path)) {
                SystemFileSystem.delete(path)
            }
        } catch (e: Exception) {
            throw FileOperationException("Failed to delete $path", e)
        }
    }

}
