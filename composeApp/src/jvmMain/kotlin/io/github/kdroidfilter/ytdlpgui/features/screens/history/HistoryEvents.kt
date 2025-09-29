package io.github.kdroidfilter.ytdlpgui.features.screens.history

sealed class HistoryEvents {
    data object Refresh : HistoryEvents()
}
