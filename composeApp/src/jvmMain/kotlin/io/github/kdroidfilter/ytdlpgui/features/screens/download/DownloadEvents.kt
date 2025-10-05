package io.github.kdroidfilter.ytdlpgui.features.screens.download

sealed class DownloadEvents {
    data object Refresh : DownloadEvents()
    data class Cancel(val id: String) : DownloadEvents()
    data object ClearHistory : DownloadEvents()
    data class DeleteHistory(val id: String) : DownloadEvents()
    data class OpenDirectory(val id: String) : DownloadEvents()
}
