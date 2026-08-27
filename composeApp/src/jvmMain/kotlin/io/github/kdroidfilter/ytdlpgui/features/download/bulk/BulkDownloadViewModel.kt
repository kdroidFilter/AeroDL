package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import io.github.kdroidfilter.youtubeplaylistextractor.YouTubePlaylistExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration

@AssistedInject
class BulkDownloadViewModel(
    @Assisted savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper,
    private val downloadManager: DownloadManager,
    private val settingsRepository: io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
) : MVIViewModel<BulkDownloadState, BulkDownloadEvents>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): BulkDownloadViewModel
    }

    override fun initialState(): BulkDownloadState = BulkDownloadState.loadingState

    val playlistUrl = normalizePlaylistUrl(savedStateHandle.toRoute<Destination.Download.Bulk>().url)

    /**
     * Normalizes YouTube URLs:
     * - Converts watch URLs with list param to playlist URLs
     */
    private fun normalizePlaylistUrl(url: String): String {
        if (url.contains("/watch") && url.contains("list=")) {
            val regex = Regex("[?&]list=([a-zA-Z0-9_-]+)")
            val listId = regex.find(url)?.groupValues?.get(1)
            if (listId != null) {
                return "https://www.youtube.com/playlist?list=$listId"
            }
        }
        return url
    }

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _playlistInfo = MutableStateFlow<PlaylistInfo?>(null)
    private val _videos = MutableStateFlow<List<BulkVideoItem>>(emptyList())
    private val _availablePresets = MutableStateFlow<List<YtDlpWrapper.Preset>>(emptyList())
    private val _selectedPreset = MutableStateFlow<YtDlpWrapper.Preset?>(null)
    private val _availableAudioQualityPresets = MutableStateFlow<List<YtDlpWrapper.AudioQualityPreset>>(emptyList())
    private val _selectedAudioQualityPreset = MutableStateFlow<YtDlpWrapper.AudioQualityPreset?>(null)
    private val _isAudioMode = MutableStateFlow(false)
    private val _isCheckingAvailability = MutableStateFlow(false)
    private val _checkedCount = MutableStateFlow(0)
    private val _navigationState = MutableStateFlow<BulkDownloadNavigationState>(BulkDownloadNavigationState.None)
    private val _isStartingDownloads = MutableStateFlow(false)
    private val _fallbackState = MutableStateFlow<FallbackState>(FallbackState.None)

    override val uiState = combine(
        _isLoading,
        _errorMessage,
        _playlistInfo,
        _videos,
        _availablePresets,
        _selectedPreset,
        _availableAudioQualityPresets,
        _selectedAudioQualityPreset,
        _isAudioMode,
        _isCheckingAvailability,
        _checkedCount,
        _navigationState,
        _isStartingDownloads,
        _fallbackState,
    ) { values: Array<Any?> ->
        val loading = values[0] as Boolean
        val error = values[1] as String?
        val playlist = values[2] as PlaylistInfo?
        @Suppress("UNCHECKED_CAST")
        val videos = values[3] as List<BulkVideoItem>
        @Suppress("UNCHECKED_CAST")
        val presets = values[4] as List<YtDlpWrapper.Preset>
        val preset = values[5] as YtDlpWrapper.Preset?
        @Suppress("UNCHECKED_CAST")
        val audioPresets = values[6] as List<YtDlpWrapper.AudioQualityPreset>
        val audioPreset = values[7] as YtDlpWrapper.AudioQualityPreset?
        val audioMode = values[8] as Boolean
        val checkingAvail = values[9] as Boolean
        val checked = values[10] as Int
        val navState = values[11] as BulkDownloadNavigationState
        val startingDownloads = values[12] as Boolean
        val fallback = values[13] as FallbackState

        BulkDownloadState(
            isLoading = loading,
            errorMessage = error,
            playlistInfo = playlist,
            videos = videos,
            availablePresets = presets,
            selectedPreset = preset,
            availableAudioQualityPresets = audioPresets,
            selectedAudioQualityPreset = audioPreset,
            isAudioMode = audioMode,
            isCheckingAvailability = checkingAvail,
            checkedCount = checked,
            navigationState = navState,
            isStartingDownloads = startingDownloads,
            fallbackState = fallback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BulkDownloadState.loadingState,
    )

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        viewModelScope.launch(Dispatchers.IO) {
            infoln { "[BulkDownloadViewModel] Loading playlist info for $playlistUrl" }
            _isLoading.value = true
            _errorMessage.value = null
            _fallbackState.value = FallbackState.None

            ytDlpWrapper.getVideoInfoList(
                url = playlistUrl,
                extractFlat = true,
                timeoutSec = 120
            )
                .onSuccess { videoList ->
                    infoln { "[BulkDownloadViewModel] Got video list successfully" }
                    infoln { "[BulkDownloadViewModel] Entries: ${videoList.size}" }
                    handleVideoListSuccess(videoList)
                }
                .onFailure { e ->
                    val detail = e.localizedMessage ?: e.message ?: e.toString()
                    errorln { "[BulkDownloadViewModel] Error getting video list: $detail" }
                    startFallback()
                }
        }
    }

    private fun handleVideoListSuccess(videoList: List<VideoInfo>, title: String = "Playlist") {
        val playlistInfo = PlaylistInfo(
            id = null,
            title = title,
            entries = videoList,
            entryCount = videoList.size
        )
        _playlistInfo.value = playlistInfo

        val shouldValidate = settingsRepository.validateBulkUrls.value

        val items = videoList.map { videoInfo ->
            BulkVideoItem(
                videoInfo = videoInfo,
                isSelected = true,
                isAvailable = true,
                isChecking = shouldValidate
            )
        }
        _videos.value = items

        setupPresets()
        _isLoading.value = false

        if (shouldValidate) {
            checkVideosAvailability(videoList)
        }
    }

    private fun setupPresets() {
        _availablePresets.value = YtDlpWrapper.Preset.entries
            .filter { it.height in listOf(360, 480, 720, 1080, 1440, 2160) }
            .sortedBy { it.height }
        _selectedPreset.value = YtDlpWrapper.Preset.P720

        _availableAudioQualityPresets.value = YtDlpWrapper.AudioQualityPreset.entries
        _selectedAudioQualityPreset.value = YtDlpWrapper.AudioQualityPreset.HIGH
    }

    private fun startFallback() {
        infoln { "[BulkDownloadViewModel] Starting HTTP playlist fallback" }
        _isLoading.value = false
        _fallbackState.value = FallbackState.Extracting(0)

        viewModelScope.launch(Dispatchers.IO) {
            YouTubePlaylistExtractor.extract(playlistUrl) { count ->
                _fallbackState.value = FallbackState.Extracting(count)
            }.onSuccess { playlist ->
                infoln { "[BulkDownloadViewModel] Fallback extraction complete: ${playlist.videos.size} videos" }
                val videoInfoList = playlist.videos.map { scraped ->
                    VideoInfo(
                        id = scraped.videoId ?: scraped.url.hashCode().toString(),
                        title = scraped.title,
                        url = scraped.url,
                        thumbnail = scraped.thumbnail,
                        duration = scraped.duration?.let { parseDuration(it) }
                    )
                }
                handleVideoListSuccess(videoInfoList, playlist.title)
                _fallbackState.value = FallbackState.Completed
            }.onFailure { e ->
                onFallbackExtractionError(e.message ?: "Failed to extract playlist")
            }
        }
    }

    private fun onFallbackExtractionError(message: String) {
        errorln { "[BulkDownloadViewModel] Fallback extraction error: $message" }
        _fallbackState.value = FallbackState.Error(message)
        _errorMessage.value = message
    }

    /**
     * Parse duration string (e.g., "1:23:45" or "12:34") to Duration.
     */
    private fun parseDuration(durationStr: String): Duration {
        val parts = durationStr.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> Duration.ofHours(parts[0].toLong())
                .plusMinutes(parts[1].toLong())
                .plusSeconds(parts[2].toLong())
            2 -> Duration.ofMinutes(parts[0].toLong())
                .plusSeconds(parts[1].toLong())
            1 -> Duration.ofSeconds(parts[0].toLong())
            else -> Duration.ZERO
        }
    }

    private fun checkVideosAvailability(entries: List<VideoInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isCheckingAvailability.value = true
            _checkedCount.value = 0

            val urls = entries.map { it.url }.filter { it.isNotBlank() }
            val resolvedIds = ytDlpWrapper.checkBatchAvailability(
                urls = urls,
                timeoutSec = 120
            ) { resolvedId ->
                _videos.value = _videos.value.map { item ->
                    if (item.videoInfo.id == resolvedId) {
                        item.copy(isAvailable = true, isChecking = false)
                    } else {
                        item
                    }
                }
                _checkedCount.value = _checkedCount.value + 1
            }

            _videos.value = _videos.value.map { item ->
                if (item.isChecking) {
                    item.copy(
                        isAvailable = false,
                        isChecking = false,
                        isSelected = false,
                        errorMessage = "Video unavailable"
                    )
                } else {
                    item
                }
            }
            _checkedCount.value = entries.size

            _isCheckingAvailability.value = false
            infoln { "[BulkDownloadViewModel] Availability check completed. Available: ${resolvedIds.size}/${entries.size}" }
        }
    }

    override fun handleEvent(event: BulkDownloadEvents) {
        when (event) {
            BulkDownloadEvents.Refresh -> loadPlaylist()

            is BulkDownloadEvents.ToggleVideoSelection -> {
                _videos.value = _videos.value.map { item ->
                    if (item.videoInfo.id == event.videoId && item.isAvailable) {
                        item.copy(isSelected = !item.isSelected)
                    } else {
                        item
                    }
                }
            }

            BulkDownloadEvents.SelectAll -> {
                _videos.value = _videos.value.map { item ->
                    if (item.isAvailable) item.copy(isSelected = true) else item
                }
            }

            BulkDownloadEvents.DeselectAll -> {
                _videos.value = _videos.value.map { item ->
                    item.copy(isSelected = false)
                }
            }

            is BulkDownloadEvents.SelectPreset -> {
                infoln { "[BulkDownloadViewModel] Preset selected: ${event.preset.height}p" }
                _selectedPreset.value = event.preset
            }

            is BulkDownloadEvents.SelectAudioQualityPreset -> {
                infoln { "[BulkDownloadViewModel] Audio quality preset selected: ${event.preset.name}" }
                _selectedAudioQualityPreset.value = event.preset
            }

            is BulkDownloadEvents.SetAudioMode -> {
                infoln { "[BulkDownloadViewModel] Audio mode set to: ${event.isAudioMode}" }
                _isAudioMode.value = event.isAudioMode
            }

            BulkDownloadEvents.StartDownloads -> {
                startDownloads()
            }

            BulkDownloadEvents.ScreenDisposed -> {
                infoln { "[BulkDownloadViewModel] Screen disposed: clearing state" }
                _playlistInfo.value = null
                _videos.value = emptyList()
                _errorMessage.value = null
                _isLoading.value = false
                _fallbackState.value = FallbackState.None
            }

            BulkDownloadEvents.OnNavigationConsumed -> {
                _navigationState.value = BulkDownloadNavigationState.None
            }

            BulkDownloadEvents.CancelFallback -> {
                infoln { "[BulkDownloadViewModel] Fallback cancelled by user" }
                _fallbackState.value = FallbackState.None
                _errorMessage.value = "Failed to load playlist"
            }
        }
    }

    private fun startDownloads() {
        val selectedVideos = _videos.value.filter { it.isSelected && it.isAvailable }
        if (selectedVideos.isEmpty()) {
            infoln { "[BulkDownloadViewModel] No videos selected for download" }
            return
        }

        _isStartingDownloads.value = true
        infoln { "[BulkDownloadViewModel] Starting downloads for ${selectedVideos.size} videos" }

        viewModelScope.launch(Dispatchers.IO) {
            val isAudio = _isAudioMode.value
            val preset = _selectedPreset.value
            val audioPreset = _selectedAudioQualityPreset.value

            selectedVideos.forEach { item ->
                val videoUrl = item.videoInfo.url
                infoln { "[BulkDownloadViewModel] Queueing download: ${item.videoInfo.title}" }

                if (isAudio) {
                    downloadManager.startAudio(
                        url = videoUrl,
                        videoInfo = item.videoInfo,
                        audioQualityPreset = audioPreset
                    )
                } else {
                    downloadManager.start(
                        url = videoUrl,
                        videoInfo = item.videoInfo,
                        preset = preset
                    )
                }
            }

            _isStartingDownloads.value = false
            _navigationState.value = BulkDownloadNavigationState.NavigateToDownloader
        }
    }
}
