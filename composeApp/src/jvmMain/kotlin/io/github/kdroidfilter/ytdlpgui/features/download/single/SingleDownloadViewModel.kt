package io.github.kdroidfilter.ytdlpgui.features.download.single

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.SubtitleInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln

class SingleDownloadViewModel(
    private val navController: NavHostController,
    private val savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val videoUrl = savedStateHandle.toRoute<Destination.Download.Single>().videoLink
    private var _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo = _videoInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _availablePresets = MutableStateFlow<List<YtDlpWrapper.Preset>>(emptyList())
    val availablePresets = _availablePresets.asStateFlow()

    private val _selectedPreset = MutableStateFlow<YtDlpWrapper.Preset?>(null)
    val selectedPreset = _selectedPreset.asStateFlow()

    private val _availableSubtitles = MutableStateFlow<Map<String, SubtitleInfo>>(emptyMap())
    val availableSubtitles = _availableSubtitles.asStateFlow()

    private val _selectedSubtitles = MutableStateFlow<List<String>>(emptyList())
    val selectedSubtitles = _selectedSubtitles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Expose a single combined state to the UI
    val state = combine(
        isLoading,
        errorMessage,
        videoInfo,
        availablePresets,
        selectedPreset,
        availableSubtitles,
        selectedSubtitles,
    ) { values: Array<Any?> ->
        val loading = values[0] as Boolean
        val error = values[1] as String?
        val info = values[2] as VideoInfo?
        @Suppress("UNCHECKED_CAST")
        val presets = values[3] as List<YtDlpWrapper.Preset>
        val preset = values[4] as YtDlpWrapper.Preset?
        @Suppress("UNCHECKED_CAST")
        val subs = values[5] as Map<String, SubtitleInfo>
        @Suppress("UNCHECKED_CAST")
        val selectedSubs = values[6] as List<String>
        SingleDownloadState(
            isLoading = loading,
            errorMessage = error,
            videoInfo = info,
            availablePresets = presets,
            selectedPreset = preset,
            availableSubtitles = subs,
            selectedSubtitles = selectedSubs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SingleDownloadState.loadingState,
    )

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            infoln { "Getting video info for $videoUrl" }
            ytDlpWrapper.getVideoInfoWithAllSubtitles(
                url = videoUrl,
                includeAutoSubtitles = true
            )
                .onSuccess { info ->
                    infoln { "[SingleDownloadViewModel] Got video info successfully" }
                    infoln { "[SingleDownloadViewModel] Title: ${info.title}" }
                    infoln { "[SingleDownloadViewModel] Available resolutions: ${info.availableResolutions}" }

                    _videoInfo.value = info

                    // Derive available presets from availableResolutions (downloadable)
                    // Now with expanded preset support including HLS/m3u8 common resolutions
                    infoln { "[SingleDownloadViewModel] Filtering presets from: ${YtDlpWrapper.Preset.entries.map { it.height }}" }
                    val presets = YtDlpWrapper.Preset.entries.filter { preset ->
                        info.availableResolutions[preset.height]?.downloadable == true
                    }.sortedBy { it.height }

                    infoln { "[SingleDownloadViewModel] Final available presets: ${presets.map { it.height }}" }

                    _availablePresets.value = presets
                    _selectedPreset.value = presets.maxByOrNull { it.height }

                    infoln { "[SingleDownloadViewModel] Selected preset: ${_selectedPreset.value?.height}p" }

                    // Subtitles - get all subtitles with their info
                    _availableSubtitles.value = info.getAllSubtitles()
                    _selectedSubtitles.value = emptyList()
                    _isLoading.value = false
                }
                .onFailure {
                    val detail = it.localizedMessage ?: it.message ?: it.toString()
                    errorln { "Error getting video info: $detail" }
                    _errorMessage.value = detail
                    _isLoading.value = false
                }
        }
    }

    fun onEvents(event: SingleDownloadEvents) {
        when (event) {
            SingleDownloadEvents.Refresh -> { /* TODO */ }
            is SingleDownloadEvents.SelectPreset -> {
                infoln { "[SingleDownloadViewModel] Preset selected: ${event.preset.height}p" }
                _selectedPreset.value = event.preset
            }
            is SingleDownloadEvents.ToggleSubtitle -> {
                val current = _selectedSubtitles.value
                _selectedSubtitles.value = if (current.contains(event.language)) {
                    infoln { "[SingleDownloadViewModel] Subtitle deselected: ${event.language}" }
                    current.filterNot { it == event.language }
                } else {
                    infoln { "[SingleDownloadViewModel] Subtitle selected: ${event.language}" }
                    current + event.language
                }
                infoln { "[SingleDownloadViewModel] Current selected subtitles: ${_selectedSubtitles.value.joinToString(",")}" }
            }
            SingleDownloadEvents.ClearSubtitles -> {
                infoln { "[SingleDownloadViewModel] Clearing all subtitle selections" }
                _selectedSubtitles.value = emptyList()
            }
            SingleDownloadEvents.StartDownload -> {
                val preset = selectedPreset.value
                val subtitles = selectedSubtitles.value
                if (subtitles.isNotEmpty()) {
                    infoln { "[SingleDownloadViewModel] Starting download with subtitles: ${subtitles.joinToString(",")}, preset: ${preset?.height}p" }
                    downloadManager.startWithSubtitles(
                        url = videoUrl,
                        videoInfo = videoInfo.value,
                        preset = preset,
                        languages = subtitles
                    )
                } else {
                    infoln { "[SingleDownloadViewModel] Starting download without subtitles, preset: ${preset?.height}p" }
                    downloadManager.start(videoUrl, videoInfo.value, preset)
                }
                viewModelScope.launch {
                    navController.navigate(Destination.MainNavigation.Downloader) {
                        popUpTo(Destination.MainNavigation.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            SingleDownloadEvents.StartAudioDownload -> {
                infoln { "[SingleDownloadViewModel] Starting audio download" }
                downloadManager.startAudio(videoUrl, videoInfo.value)
                viewModelScope.launch {
                    navController.navigate(Destination.MainNavigation.Downloader) {
                        popUpTo(Destination.MainNavigation.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            SingleDownloadEvents.ScreenDisposed -> {
                infoln { "[SingleDownloadViewModel] Screen disposed: clearing heavy state" }
                // Clear heavy state to release references quickly
                _videoInfo.value = null
                _availablePresets.value = emptyList()
                _selectedPreset.value = null
                _availableSubtitles.value = emptyMap()
                _selectedSubtitles.value = emptyList()
                _errorMessage.value = null
                _isLoading.value = false

                // Hint GC after dereferencing large objects
                try {
                    System.gc()
                } catch (_: Throwable) {
                    // ignore
                }
            }
        }
    }
}
