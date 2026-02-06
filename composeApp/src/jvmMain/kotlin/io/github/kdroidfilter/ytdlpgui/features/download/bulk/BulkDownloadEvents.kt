package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import io.github.kdroidfilter.ytdlp.YtDlpWrapper

sealed class BulkDownloadEvents {
    data object Refresh : BulkDownloadEvents()
    data class ToggleVideoSelection(val videoId: String) : BulkDownloadEvents()
    data object SelectAll : BulkDownloadEvents()
    data object DeselectAll : BulkDownloadEvents()
    data class SelectPreset(val preset: YtDlpWrapper.Preset) : BulkDownloadEvents()
    data class SelectAudioQualityPreset(val preset: YtDlpWrapper.AudioQualityPreset) : BulkDownloadEvents()
    data class SetAudioMode(val isAudioMode: Boolean) : BulkDownloadEvents()
    data object StartDownloads : BulkDownloadEvents()
    data object ScreenDisposed : BulkDownloadEvents()
    data object OnNavigationConsumed : BulkDownloadEvents()

    // Fallback WebView events
    data object OnUserLoggedIn : BulkDownloadEvents()
    data object OnFallbackExtractionComplete : BulkDownloadEvents()
    data object CancelFallback : BulkDownloadEvents()
}
