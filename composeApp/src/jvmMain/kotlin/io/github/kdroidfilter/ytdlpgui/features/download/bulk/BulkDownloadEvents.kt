package io.github.kdroidfilter.ytdlpgui.features.download.bulk

sealed class BulkDownloadEvents {
    data object Refresh : BulkDownloadEvents()
}
