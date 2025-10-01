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

    val videoUrl = savedStateHandle.toRoute<Destination.SingleDownloadScreen>().videoLink
    private var _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo = _videoInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _availablePresets = MutableStateFlow<List<YtDlpWrapper.Preset>>(emptyList())
    val availablePresets = _availablePresets.asStateFlow()

    private val _selectedPreset = MutableStateFlow<YtDlpWrapper.Preset?>(null)
    val selectedPreset = _selectedPreset.asStateFlow()

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            println("Getting video info for $videoUrl")
            ytDlpWrapper.getVideoInfo(videoUrl)
                .onSuccess { info ->
                    _videoInfo.value = info
                    // Derive available presets from availableResolutions (downloadable)
                    val presets = YtDlpWrapper.Preset.entries.filter { preset ->
                        info.availableResolutions[preset.height]?.downloadable == true
                    }.sortedBy { it.height }
                    _availablePresets.value = presets
                    _selectedPreset.value = presets.maxByOrNull { it.height }
                    _isLoading.value = false
                }
                .onFailure {
                    println("Error getting video info: ${it.message}")
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
            SingleDownloadEvents.StartDownload -> {
                downloadManager.start(videoUrl, videoInfo.value, selectedPreset.value)
                viewModelScope.launch {
                    navigator.navigate(Destination.HistoryScreen)
                }
            }
        }
    }
}
