package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

sealed class SingleDownloadEvents {
    data object Refresh : SingleDownloadEvents()
    data object StartDownload : SingleDownloadEvents()
}
