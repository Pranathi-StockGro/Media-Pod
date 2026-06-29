package com.stockgro.androidsampleapp.ui.prefetch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.DataSource
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.PrefetchMediaType
import com.stockgro.prefetch.PrefetchStrategy
import com.stockgro.prefetch.data.PrefetchDatabase
import com.stockgro.prefetch.datasource.ChunkMergerDataSourceFactory
import com.stockgro.mediapod.data.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

enum class ChunkStatus {
    DOWNLOADED, IN_FLIGHT, MISSING
}

data class ChunkVisualState(
    val index: Int,
    val status: ChunkStatus,
)

data class VideoDiagnosticState(
    val url: String,
    val title: String,
    val totalSize: Long = 0,
    val chunks: List<ChunkVisualState> = emptyList(),
    val prefetchStatus: String = "Idle"
)

class PrefetchTestViewModel(
    private val prefetchManager: MediaPrefetchManager,
    private val database: PrefetchDatabase,
    private val interceptor: DiagnosticInterceptor
) : ViewModel() {

    private val _videoStates = MutableStateFlow(
        SampleData.videos.map { (url, title) -> VideoDiagnosticState(url, title) }
    )
    val videoStates: StateFlow<List<VideoDiagnosticState>> = _videoStates.asStateFlow()

    private val _isDelayEnabled = MutableStateFlow(false)
    val isDelayEnabled = _isDelayEnabled.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                updateStates()
                delay(500.milliseconds)
            }
        }
    }

    private suspend fun updateStates() {
        val dao = database.prefetchDao()
        val inFlight = interceptor.inFlightChunks

        val newStates = _videoStates.value.map { state ->
            val metadata = dao.getMetadata(state.url)
            if (metadata != null) {
                val allChunks = dao.getAllChunksForUrl(state.url)
                val chunkSize = metadata.chunkSize.toLong()
                val totalChunks = (((metadata.totalSize + chunkSize) - 1) / chunkSize).toInt()

                val visualChunks = (0 until totalChunks).map { index ->
                    val chunk = allChunks.find { it.chunkIndex == index }
                    val status = when {
                        chunk?.isCompleted == true -> ChunkStatus.DOWNLOADED
                        inFlight.contains(state.url to index) -> ChunkStatus.IN_FLIGHT
                        else -> ChunkStatus.MISSING
                    }
                    ChunkVisualState(index, status)
                }

                state.copy(
                    totalSize = metadata.totalSize,
                    chunks = visualChunks,
                    prefetchStatus = if (visualChunks.all { it.status == ChunkStatus.DOWNLOADED }) "Cached" else if (visualChunks.any { it.status == ChunkStatus.DOWNLOADED }) "Partial" else "Idle"
                )
            } else {
                state
            }
        }
        _videoStates.value = newStates
    }

    fun toggleDelay(enabled: Boolean) {
        _isDelayEnabled.value = enabled
        // Removed debugDelayBetweenChunks as requested
    }

    fun prefetchVideo(url: String) {
        viewModelScope.launch {
            prefetchManager.prefetchVideos(listOf(url), PrefetchMediaType.MP4, PrefetchStrategy.Full)
        }
    }

    fun clearAllCaches() {
        viewModelScope.launch {
            prefetchManager.clearAllCaches()
            updateStates()
        }
    }

    fun getProxyUrl(url: String): String {
        return url
    }

    fun createDataSourceFactory(url: String): DataSource.Factory {
        return ChunkMergerDataSourceFactory(null) {
            runBlocking { prefetchManager.getChunkMerger(url) }
        }
    }
}
