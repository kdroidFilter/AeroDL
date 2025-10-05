package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SingleDownloadViewModel(
    private val navigator: Navigator,
    private val savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper,
    private val downloadManager: io.github.kdroidfilter.ytdlpgui.features.screens.download.DownloadManager,
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

    private val _availableSubtitleLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableSubtitleLanguages = _availableSubtitleLanguages.asStateFlow()

    private val _selectedSubtitles = MutableStateFlow<List<String>>(emptyList())
    val selectedSubtitles = _selectedSubtitles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            println("Getting video info for $videoUrl")
            ytDlpWrapper.getVideoInfoWithAllSubtitles(
                url = videoUrl,
                includeAutoSubtitles = true
            )
                .onSuccess { info ->
                    _videoInfo.value = info
                    // Derive available presets from availableResolutions (downloadable)
                    val presets = YtDlpWrapper.Preset.entries.filter { preset ->
                        info.availableResolutions[preset.height]?.downloadable == true
                    }.sortedBy { it.height }
                    _availablePresets.value = presets
                    _selectedPreset.value = presets.maxByOrNull { it.height }
                    // Subtitles
                    val allLangs = info.getAllSubtitleLanguages()
                    _availableSubtitleLanguages.value = allLangs.sorted()
                    _selectedSubtitles.value = emptyList()
                    _isLoading.value = false
                }
                .onFailure {
                    val detail = it.localizedMessage ?: it.message ?: it.toString()
                    println("Error getting video info: $detail")
                    _errorMessage.value = detail
                    _isLoading.value = false
                }
        }
    }

    fun onEvents(event: SingleDownloadEvents) {
        when (event) {
            SingleDownloadEvents.Refresh -> { /* TODO */ }
            is SingleDownloadEvents.SelectPreset -> {
                _selectedPreset.value = event.preset
            }
            is SingleDownloadEvents.ToggleSubtitle -> {
                val current = _selectedSubtitles.value
                _selectedSubtitles.value = if (current.contains(event.language)) {
                    current.filterNot { it == event.language }
                } else {
                    current + event.language
                }
            }
            SingleDownloadEvents.ClearSubtitles -> {
                _selectedSubtitles.value = emptyList()
            }
            SingleDownloadEvents.StartDownload -> {
                val preset = selectedPreset.value
                val subtitles = selectedSubtitles.value
                if (subtitles.isNotEmpty()) {
                    downloadManager.startWithSubtitles(
                        url = videoUrl,
                        videoInfo = videoInfo.value,
                        preset = preset,
                        languages = subtitles
                    )
                } else {
                    downloadManager.start(videoUrl, videoInfo.value, preset)
                }
                viewModelScope.launch {
                    navigator.navigate(Destination.HistoryScreen)
                }
            }
            SingleDownloadEvents.StartAudioDownload -> {
                downloadManager.startAudio(videoUrl, videoInfo.value)
                viewModelScope.launch {
                    navigator.navigate(Destination.HistoryScreen)
                }
            }
        }
    }
}
