package io.github.kdroidfilter.ytdlpgui.features.download.single

import io.github.kdroidfilter.ytdlp.YtDlpWrapper

sealed class SingleDownloadEvents {
    data object Refresh : SingleDownloadEvents()
    data class SelectPreset(val preset: YtDlpWrapper.Preset) : SingleDownloadEvents()
    data class SelectAudioQualityPreset(val preset: YtDlpWrapper.AudioQualityPreset) : SingleDownloadEvents()
    data class ToggleSubtitle(val language: String) : SingleDownloadEvents()
    data object ClearSubtitles : SingleDownloadEvents()
    data object StartDownload : SingleDownloadEvents()
    data object StartAudioDownload : SingleDownloadEvents()
    data object ScreenDisposed : SingleDownloadEvents()
}
