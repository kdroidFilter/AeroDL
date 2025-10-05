package io.github.kdroidfilter.ytdlpgui.features.screens.download.bulkdownload

sealed class BulkDownloadEvents {
    data object Refresh : BulkDownloadEvents()
}
