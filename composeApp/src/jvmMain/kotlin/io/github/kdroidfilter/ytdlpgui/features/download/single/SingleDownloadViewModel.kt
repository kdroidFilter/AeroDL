package io.github.kdroidfilter.ytdlpgui.features.download.single

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.SubtitleInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln

class SingleDownloadViewModel(
    savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper,
    private val downloadManager: DownloadManager,
) : MVIViewModel<SingleDownloadState, SingleDownloadEvents>(savedStateHandle) {


    override fun initialState(): SingleDownloadState = SingleDownloadState.loadingState

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

    private val _availableAudioQualityPresets = MutableStateFlow<List<YtDlpWrapper.AudioQualityPreset>>(emptyList())
    val availableAudioQualityPresets = _availableAudioQualityPresets.asStateFlow()

    private val _selectedAudioQualityPreset = MutableStateFlow<YtDlpWrapper.AudioQualityPreset?>(null)
    val selectedAudioQualityPreset = _selectedAudioQualityPreset.asStateFlow()

    private val _splitChapters = MutableStateFlow(false)
    val splitChapters = _splitChapters.asStateFlow()

    private val _hasSponsorSegments = MutableStateFlow(false)
    val hasSponsorSegments = _hasSponsorSegments.asStateFlow()

    private val _removeSponsors = MutableStateFlow(false)
    val removeSponsors = _removeSponsors.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _navigationState = MutableStateFlow<SingleDownloadNavigationState>(SingleDownloadNavigationState.None)
    val navigationState = _navigationState.asStateFlow()

    // Note: This ViewModel uses a combined state from multiple sources, so we override uiState
    override val uiState = combine(
        isLoading,
        errorMessage,
        videoInfo,
        availablePresets,
        selectedPreset,
        availableSubtitles,
        selectedSubtitles,
        availableAudioQualityPresets,
        selectedAudioQualityPreset,
        splitChapters,
        hasSponsorSegments,
        removeSponsors,
        navigationState,
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
        @Suppress("UNCHECKED_CAST")
        val audioPresets = values[7] as List<YtDlpWrapper.AudioQualityPreset>
        val audioPreset = values[8] as YtDlpWrapper.AudioQualityPreset?
        val split = values[9] as Boolean
        val sponsorAvail = values[10] as Boolean
        val rmSponsor = values[11] as Boolean
        val navState = values[12] as SingleDownloadNavigationState
        SingleDownloadState(
            isLoading = loading,
            errorMessage = error,
            videoInfo = info,
            availablePresets = presets,
            selectedPreset = preset,
            availableSubtitles = subs,
            selectedSubtitles = selectedSubs,
            availableAudioQualityPresets = audioPresets,
            selectedAudioQualityPreset = audioPreset,
            splitChapters = split,
            hasSponsorSegments = sponsorAvail,
            removeSponsors = rmSponsor,
            navigationState = navState,
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

                    // Audio quality presets - all available
                    _availableAudioQualityPresets.value = YtDlpWrapper.AudioQualityPreset.entries
                    _selectedAudioQualityPreset.value = YtDlpWrapper.AudioQualityPreset.HIGH

                    _isLoading.value = false
                }
                .onFailure {
                    val detail = it.localizedMessage ?: it.message ?: it.toString()
                    errorln { "Error getting video info: $detail" }
                    _errorMessage.value = detail
                    _isLoading.value = false
                }
        }
        // Fire-and-forget sponsor detection (does not block UI)
        scope.launch {
            ytDlpWrapper.detectSponsorSegments(videoUrl)
                .onSuccess { has -> _hasSponsorSegments.value = has }
                .onFailure { _hasSponsorSegments.value = false }
        }
    }

    override fun handleEvent(event: SingleDownloadEvents) {
        when (event) {
            SingleDownloadEvents.Refresh -> { /* TODO */ }
            is SingleDownloadEvents.SelectPreset -> {
                infoln { "[SingleDownloadViewModel] Preset selected: ${event.preset.height}p" }
                _selectedPreset.value = event.preset
            }
            is SingleDownloadEvents.SelectAudioQualityPreset -> {
                infoln { "[SingleDownloadViewModel] Audio quality preset selected: ${event.preset.name}" }
                _selectedAudioQualityPreset.value = event.preset
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
            is SingleDownloadEvents.SetSplitChapters -> {
                infoln { "[SingleDownloadViewModel] Split chapters set to: ${event.enabled}" }
                _splitChapters.value = event.enabled
            }
            is SingleDownloadEvents.SetRemoveSponsors -> {
                infoln { "[SingleDownloadViewModel] Remove sponsors set to: ${event.enabled}" }
                _removeSponsors.value = event.enabled
            }
            SingleDownloadEvents.StartDownload -> {
                val preset = selectedPreset.value
                val subtitles = selectedSubtitles.value
                if (_splitChapters.value) {
                    if (subtitles.isNotEmpty()) {
                        infoln { "[SingleDownloadViewModel] Starting split-chapters download with subtitles: ${subtitles.joinToString(",")}, preset: ${preset?.height}p" }
                        downloadManager.startSplitChapters(
                            url = videoUrl,
                            videoInfo = videoInfo.value,
                            preset = preset,
                            languages = subtitles,
                            sponsorBlock = _removeSponsors.value
                        )
                    } else {
                        infoln { "[SingleDownloadViewModel] Starting split-chapters download without subtitles, preset: ${preset?.height}p" }
                        downloadManager.startSplitChapters(
                            url = videoUrl,
                            videoInfo = videoInfo.value,
                            preset = preset,
                            languages = null,
                            sponsorBlock = _removeSponsors.value
                        )
                    }
                } else {
                    if (subtitles.isNotEmpty()) {
                        infoln { "[SingleDownloadViewModel] Starting download with subtitles: ${subtitles.joinToString(",")}, preset: ${preset?.height}p" }
                        downloadManager.startWithSubtitles(
                            url = videoUrl,
                            videoInfo = videoInfo.value,
                            preset = preset,
                            languages = subtitles,
                            sponsorBlock = _removeSponsors.value
                        )
                    } else {
                        infoln { "[SingleDownloadViewModel] Starting download without subtitles, preset: ${preset?.height}p" }
                        downloadManager.start(videoUrl, videoInfo.value, preset, sponsorBlock = _removeSponsors.value)
                    }
                }
                _navigationState.value = SingleDownloadNavigationState.NavigateToDownloader
            }
            SingleDownloadEvents.StartAudioDownload -> {
                val audioQuality = selectedAudioQualityPreset.value
                if (_splitChapters.value) {
                    infoln { "[SingleDownloadViewModel] Starting audio split-chapters download with quality: ${audioQuality?.name}" }
                    downloadManager.startAudioSplitChapters(videoUrl, videoInfo.value, audioQuality, sponsorBlock = _removeSponsors.value)
                } else {
                    infoln { "[SingleDownloadViewModel] Starting audio download with quality: ${audioQuality?.name}" }
                    downloadManager.startAudio(videoUrl, videoInfo.value, audioQuality, sponsorBlock = _removeSponsors.value)
                }
                _navigationState.value = SingleDownloadNavigationState.NavigateToDownloader
            }
            SingleDownloadEvents.StartSplitChaptersDownload -> {
                val preset = selectedPreset.value
                val subtitles = selectedSubtitles.value
                if (subtitles.isNotEmpty()) {
                    infoln { "[SingleDownloadViewModel] Starting split-chapters download with subtitles: ${subtitles.joinToString(",")}, preset: ${preset?.height}p" }
                    downloadManager.startSplitChapters(
                        url = videoUrl,
                        videoInfo = videoInfo.value,
                        preset = preset,
                        languages = subtitles
                    )
                } else {
                    infoln { "[SingleDownloadViewModel] Starting split-chapters download without subtitles, preset: ${preset?.height}p" }
                    downloadManager.startSplitChapters(
                        url = videoUrl,
                        videoInfo = videoInfo.value,
                        preset = preset,
                        languages = null
                    )
                }
                _navigationState.value = SingleDownloadNavigationState.NavigateToDownloader
            }
            SingleDownloadEvents.StartAudioSplitChaptersDownload -> {
                val audioQuality = selectedAudioQualityPreset.value
                infoln { "[SingleDownloadViewModel] Starting audio split-chapters download with quality: ${audioQuality?.name}" }
                downloadManager.startAudioSplitChapters(videoUrl, videoInfo.value, audioQuality)
                _navigationState.value = SingleDownloadNavigationState.NavigateToDownloader
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
            SingleDownloadEvents.OnNavigationConsumed -> {
                _navigationState.value = SingleDownloadNavigationState.None
            }
        }
    }
}
