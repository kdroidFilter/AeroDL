package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import io.github.kdroidfilter.ytdlp.YtDlpWrapper

sealed class SingleDownloadEvents {
    data object Refresh : SingleDownloadEvents()
    data class SelectPreset(val preset: YtDlpWrapper.Preset) : SingleDownloadEvents()
    data object StartDownload : SingleDownloadEvents()
    data object StartAudioDownload : SingleDownloadEvents()
}
