package io.github.kdroidfilter.ytdlpgui.features.screens.settings

sealed class SettingsEvents {

    data object Refresh : SettingsEvents()

    data class SetNotifyOnComplete(val enabled: Boolean) : SettingsEvents()
    data class SetNoCheckCertificate(val enabled: Boolean) : SettingsEvents()
    data class SetCookiesFromBrowser(val browser: String) : SettingsEvents()
    data class SetIncludePresetInFilename(val enabled: Boolean) : SettingsEvents()
    data class SetParallelDownloads(val count: Int) : SettingsEvents()
    data class SetDownloadDir(val path: String) : SettingsEvents()
    data class SetClipboardMonitoring(val enabled: Boolean) : SettingsEvents()
    data class SetAutoLaunchEnabled(val enabled: Boolean) : SettingsEvents()
    data class PickDownloadDir(val title: String) : SettingsEvents()
}
