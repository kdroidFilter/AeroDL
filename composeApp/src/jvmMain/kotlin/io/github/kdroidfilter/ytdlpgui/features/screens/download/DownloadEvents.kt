package io.github.kdroidfilter.ytdlpgui.features.screens.download

sealed class DownloadEvents {
    data object Refresh : DownloadEvents()
    data class Cancel(val id: String) : DownloadEvents()
}
