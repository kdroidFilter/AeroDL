package io.github.kdroidfilter.ytdlpgui.features.screens.settings

sealed class SettingsEvents {
    data object Refresh : SettingsEvents()
}
