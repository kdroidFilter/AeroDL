package io.github.kdroidfilter.ytdlpgui.features.download.single

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.SubtitleInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.kdroidfilter.ytdlpgui.core.util.errorln
import io.github.kdroidfilter.ytdlpgui.core.util.infoln

class SingleDownloadViewModel(
    private val navigator: Navigator,
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

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            infoln { "Getting video info for $videoUrl" }
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
                    navigator.navigateAndClearBackStack(Destination.MainNavigation.Downloader)
                }
            }
            SingleDownloadEvents.StartAudioDownload -> {
                infoln { "[SingleDownloadViewModel] Starting audio download" }
                downloadManager.startAudio(videoUrl, videoInfo.value)
                viewModelScope.launch {
                    navigator.navigateAndClearBackStack(Destination.MainNavigation.Downloader)
                }
            }
        }
    }
}
