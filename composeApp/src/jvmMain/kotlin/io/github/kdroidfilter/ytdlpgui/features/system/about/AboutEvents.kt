package io.github.kdroidfilter.ytdlpgui.features.system.about

sealed class AboutEvents {
    data object Refresh : AboutEvents()
}
