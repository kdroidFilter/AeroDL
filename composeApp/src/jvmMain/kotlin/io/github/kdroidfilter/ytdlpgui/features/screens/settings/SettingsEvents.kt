package io.github.kdroidfilter.ytdlpgui.features.screens.settings

sealed class SettingsEvents {
    data object Refresh : SettingsEvents()
    data class SetNoCheckCertificate(val enabled: Boolean) : SettingsEvents()
    data class SetCookiesFromBrowser(val browser: String) : SettingsEvents()
}
