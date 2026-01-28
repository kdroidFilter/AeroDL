package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.youtubewebviewextractor.YouTubeWebViewExtractor

/**
 * Represents a single video entry in the bulk download list with its selection and availability state.
 */
data class BulkVideoItem(
    val videoInfo: VideoInfo,
    val isSelected: Boolean = true,
    val isAvailable: Boolean = true,
    val isChecking: Boolean = false,
    val errorMessage: String? = null
)

sealed class BulkDownloadNavigationState {
    data object None : BulkDownloadNavigationState()
    data object NavigateToDownloader : BulkDownloadNavigationState()
}

/**
 * Represents the fallback extraction state when yt-dlp fails.
 */
sealed class FallbackState {
    /** No fallback needed, yt-dlp worked */
    data object None : FallbackState()

    /** Checking if user is logged in to YouTube */
    data object CheckingLogin : FallbackState()

    /** User needs to log in to YouTube */
    data object LoginRequired : FallbackState()

    /** Extracting videos via WebView */
    data class Extracting(val videoCount: Int) : FallbackState()

    /** Fallback extraction completed */
    data object Completed : FallbackState()

    /** Fallback extraction failed */
    data class Error(val message: String) : FallbackState()
}

data class BulkDownloadState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val playlistInfo: PlaylistInfo? = null,
    val videos: List<BulkVideoItem> = emptyList(),
    val availablePresets: List<YtDlpWrapper.Preset> = emptyList(),
    val selectedPreset: YtDlpWrapper.Preset? = null,
    val availableAudioQualityPresets: List<YtDlpWrapper.AudioQualityPreset> = emptyList(),
    val selectedAudioQualityPreset: YtDlpWrapper.AudioQualityPreset? = null,
    val isAudioMode: Boolean = false,
    val isCheckingAvailability: Boolean = false,
    val checkedCount: Int = 0,
    val navigationState: BulkDownloadNavigationState = BulkDownloadNavigationState.None,
    val isStartingDownloads: Boolean = false,
    val fallbackState: FallbackState = FallbackState.None,
    val webViewExtractor: YouTubeWebViewExtractor? = null
) {
    val selectedCount: Int
        get() = videos.count { it.isSelected && it.isAvailable }

    val totalCount: Int
        get() = videos.size

    val availableCount: Int
        get() = videos.count { it.isAvailable }

    val allSelected: Boolean
        get() = videos.all { !it.isAvailable || it.isSelected }

    val noneSelected: Boolean
        get() = videos.none { it.isSelected && it.isAvailable }

    companion object {
        val loadingState = BulkDownloadState(isLoading = true)
        val emptyState = BulkDownloadState(isLoading = false)
        val errorState = BulkDownloadState(
            isLoading = false,
            errorMessage = "Failed to load playlist information"
        )
    }
}
