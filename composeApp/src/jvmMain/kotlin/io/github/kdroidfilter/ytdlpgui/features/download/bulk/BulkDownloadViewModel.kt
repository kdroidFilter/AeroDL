@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
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
import io.github.kdroidfilter.youtubewebviewextractor.YouTubeScrapedVideo
import io.github.kdroidfilter.youtubewebviewextractor.YouTubeWebViewExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.kdroidfilter.network.HttpsConnectionFactory
import java.time.Duration

class BulkDownloadViewModel @AssistedInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper,
    private val downloadManager: DownloadManager,
    private val trayAppState: TrayAppState
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
        // If it's a watch URL with a playlist ID, convert to playlist URL
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
    private val _webViewExtractor = MutableStateFlow<YouTubeWebViewExtractor?>(null)

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
        _webViewExtractor,
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
        val extractor = values[14] as YouTubeWebViewExtractor?

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
            webViewExtractor = extractor,
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
                    // Trigger fallback instead of showing error
                    startFallback()
                }
        }
    }

    private fun handleVideoListSuccess(videoList: List<VideoInfo>) {
        val playlistInfo = PlaylistInfo(
            id = null,
            title = "Playlist",
            entries = videoList,
            entryCount = videoList.size
        )
        _playlistInfo.value = playlistInfo

        val items = videoList.map { videoInfo ->
            BulkVideoItem(
                videoInfo = videoInfo,
                isSelected = true,
                isAvailable = true,
                isChecking = true
            )
        }
        _videos.value = items

        setupPresets()
        _isLoading.value = false

        checkVideosAvailability(videoList)
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
        infoln { "[BulkDownloadViewModel] Starting WebView fallback extraction" }
        _isLoading.value = false
        _fallbackState.value = FallbackState.CheckingLogin

        // Set MANUAL dismiss mode to prevent app from closing when new window is created
        trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)

        // Create extractor instance
        val extractor = YouTubeWebViewExtractor()
        _webViewExtractor.value = extractor
    }

    /**
     * Called by the UI when the WebView has checked login status.
     * If logged in, extraction will start automatically.
     * If not logged in, LoginRequired state is set.
     */
    fun onLoginStatusChecked(isLoggedIn: Boolean?) {
        infoln { "[BulkDownloadViewModel] Login status checked: $isLoggedIn" }
        when (isLoggedIn) {
            true -> {
                // User is logged in, start extraction
                _fallbackState.value = FallbackState.Extracting(0)
            }
            else -> {
                // User needs to log in (treat null/unknown as not logged in)
                _fallbackState.value = FallbackState.LoginRequired
            }
        }
    }

    /**
     * Called by the UI when user has logged in via the WebView.
     */
    fun onUserLoggedIn() {
        infoln { "[BulkDownloadViewModel] User logged in, starting extraction" }
        _fallbackState.value = FallbackState.Extracting(0)
    }

    /**
     * Called by the UI to update extraction progress.
     */
    fun onExtractionProgress(videoCount: Int) {
        _fallbackState.value = FallbackState.Extracting(videoCount)
    }

    /**
     * Called by the UI when fallback extraction is complete.
     */
    fun onFallbackExtractionComplete(videos: List<YouTubeScrapedVideo>) {
        infoln { "[BulkDownloadViewModel] Fallback extraction complete: ${videos.size} videos" }

        // Restore AUTO dismiss mode now that WebView window is no longer needed
        trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)

        // Convert YouTubeScrapedVideo to VideoInfo
        val videoInfoList = videos.map { scraped ->
            VideoInfo(
                id = scraped.videoId ?: scraped.url.hashCode().toString(),
                title = scraped.title,
                url = scraped.url,
                thumbnail = scraped.thumbnail,
                duration = scraped.duration?.let { parseDuration(it) }
            )
        }

        handleVideoListSuccess(videoInfoList)
        _fallbackState.value = FallbackState.Completed
    }

    /**
     * Called by the UI when fallback extraction fails.
     */
    fun onFallbackExtractionError(message: String) {
        errorln { "[BulkDownloadViewModel] Fallback extraction error: $message" }
        trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
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

            entries.forEachIndexed { index, videoInfo ->
                val isAvailable = checkVideoAvailability(videoInfo)

                _videos.value = _videos.value.map { item ->
                    if (item.videoInfo.id == videoInfo.id) {
                        item.copy(
                            isAvailable = isAvailable,
                            isChecking = false,
                            isSelected = isAvailable,
                            errorMessage = if (!isAvailable) "Video unavailable" else null
                        )
                    } else {
                        item
                    }
                }
                _checkedCount.value = index + 1
            }

            _isCheckingAvailability.value = false
            infoln { "[BulkDownloadViewModel] Availability check completed. Available: ${_videos.value.count { it.isAvailable }}/${entries.size}" }
        }
    }

    private suspend fun checkVideoAvailability(videoInfo: VideoInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val urlToCheck = videoInfo.url
                if (urlToCheck.isBlank()) return@withContext false

                val connection = HttpsConnectionFactory.openConnection(urlToCheck) {
                    requestMethod = "HEAD"
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; AeroDL)")
                }

                try {
                    connection.connect()
                    val responseCode = connection.responseCode
                    connection.disconnect()
                    responseCode in 200..399
                } catch (e: Exception) {
                    connection.disconnect()
                    false
                }
            } catch (e: Exception) {
                infoln { "[BulkDownloadViewModel] URL check failed for ${videoInfo.id}: ${e.message}, assuming unavailable" }
                false
            }
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
                trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
                _playlistInfo.value = null
                _videos.value = emptyList()
                _errorMessage.value = null
                _isLoading.value = false
                _fallbackState.value = FallbackState.None
                _webViewExtractor.value?.reset()
                _webViewExtractor.value = null
            }

            BulkDownloadEvents.OnNavigationConsumed -> {
                _navigationState.value = BulkDownloadNavigationState.None
            }

            BulkDownloadEvents.OnUserLoggedIn -> {
                onUserLoggedIn()
            }

            BulkDownloadEvents.OnFallbackExtractionComplete -> {
                val extractor = _webViewExtractor.value
                if (extractor != null) {
                    onFallbackExtractionComplete(extractor.extractedVideos)
                }
            }

            BulkDownloadEvents.CancelFallback -> {
                infoln { "[BulkDownloadViewModel] Fallback cancelled by user" }
                trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
                _fallbackState.value = FallbackState.None
                _webViewExtractor.value?.reset()
                _webViewExtractor.value = null
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

    override fun onCleared() {
        super.onCleared()
        trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
    }
}
