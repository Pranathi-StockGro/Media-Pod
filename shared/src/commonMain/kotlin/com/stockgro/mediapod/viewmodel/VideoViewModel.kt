package com.stockgro.mediapod.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.PrefetchMediaType
import com.stockgro.prefetch.PrefetchStatus
import com.stockgro.prefetch.PrefetchStrategy
import com.stockgro.mediapod.data.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoViewState(
    val videos: List<Pair<String, String>> = SampleData.videos,
    val selectedUrl: String? = null,
    val prefetchStatuses: Map<String, PrefetchStatus> = emptyMap()
)

sealed class VideoIntent {
    data class SelectVideo(val url: String) : VideoIntent()
    data class PrefetchVideo(val url: String) : VideoIntent()
    object ClearAllCaches : VideoIntent()
}

class VideoViewModel(
    val prefetchManager: MediaPrefetchManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoViewState())
    val uiState: StateFlow<VideoViewState> = _uiState.asStateFlow()

    init {
        observePrefetchStatuses()
    }

    fun onIntent(intent: VideoIntent) {
        when (intent) {
            is VideoIntent.SelectVideo -> selectVideo(intent.url)
            is VideoIntent.PrefetchVideo -> prefetchVideo(intent.url)
            is VideoIntent.ClearAllCaches -> clearCaches()
        }
    }

    private fun observePrefetchStatuses() {
        viewModelScope.launch {
            prefetchManager.statusMap.collect { statuses ->
                _uiState.update { it.copy(prefetchStatuses = statuses) }
            }
        }
    }

    private fun selectVideo(url: String) {
        _uiState.update { it.copy(selectedUrl = url) }
    }

    private fun prefetchVideo(url: String) {
        viewModelScope.launch {
            prefetchManager.prefetchVideos(
                urls = listOf(url),
                type = PrefetchMediaType.MP4,
                strategy = PrefetchStrategy.Full
            )
        }
    }

    private fun clearCaches() {
        viewModelScope.launch {
            prefetchManager.clearAllCaches()
        }
    }
}
