package io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload

sealed class BulkDownloadEvents {
    data object Refresh : BulkDownloadEvents()
}
