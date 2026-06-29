package com.stockgro.prefetch.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.runBlocking
import java.io.IOException

@UnstableApi
class ChunkMergerDataSource(
    private val chunkMerger: ChunkMerger
) : BaseDataSource(false) {

    private var dataSpec: DataSpec? = null
    private var opened = false
    private var currentPosition = 0L
    private var bytesRemaining = 0L

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.currentPosition = dataSpec.position

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

        val bytesRead = runBlocking {
            chunkMerger.read(currentPosition, bytesToRead, buffer, offset)
        }

        if (bytesRead == -1) {
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRead == 0) {
            throw IOException("Failed to read data at position $currentPosition. Chunk might be missing or download failed.")
        }

        currentPosition += bytesRead
        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        dataSpec = null
    }
}

class ChunkMergerDataSourceFactory(
    private val chunkMergerProvider: (String) -> ChunkMerger
) : DataSource.Factory {

    @UnstableApi
    override fun createDataSource(): DataSource {
        return ChunkMergerDataSource(chunkMergerProvider("todo"))
    }
}
