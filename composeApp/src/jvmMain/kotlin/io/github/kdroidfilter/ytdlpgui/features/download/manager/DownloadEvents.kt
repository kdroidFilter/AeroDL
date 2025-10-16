package io.github.kdroidfilter.ytdlpgui.features.download.manager

sealed class DownloadEvents {
    data object Refresh : DownloadEvents()
    data class Cancel(val id: String) : DownloadEvents()
    data object ClearHistory : DownloadEvents()
    data class DeleteHistory(val id: String) : DownloadEvents()
    data class OpenDirectory(val id: String) : DownloadEvents()
    data class ShowErrorDialog(val id: String) : DownloadEvents()
    data object DismissErrorDialog : DownloadEvents()
    data class DismissFailed(val id: String) : DownloadEvents()
    data object DismissUpdateInfoBar : DownloadEvents()
    data class UpdateSearchQuery(val query: String) : DownloadEvents()
}
