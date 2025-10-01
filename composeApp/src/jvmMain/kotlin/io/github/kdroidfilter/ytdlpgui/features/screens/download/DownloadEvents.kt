package io.github.kdroidfilter.ytdlpgui.features.screens.download

sealed class DownloadEvents {
    data object Refresh : DownloadEvents()
}
