package io.github.kdroidfilter.ytdlpgui.features.system.settings

// UI state for the Settings screen
// - noCheckCertificate: when true, yt-dlp will be called with --no-check-certificates
// - cookiesFromBrowser: the browser name to use for --cookies-from-browser (e.g., "firefox", "chrome"), empty to disable

data class SettingsState(
    val isLoading: Boolean = false,
    val noCheckCertificate: Boolean = false,
    val cookiesFromBrowser: String = "",
    val includePresetInFilename: Boolean = true,
    val embedThumbnailInMp3: Boolean = true,
    val parallelDownloads: Int = 2,
    val downloadDirPath: String = "",
    val clipboardMonitoringEnabled: Boolean = false,
    val autoLaunchEnabled: Boolean = false,
    val notifyOnComplete: Boolean = true,
    val concurrentFragments: Int = 1,
) {
    companion object {
        val defaultState = SettingsState()

        val customizedState = SettingsState(
            noCheckCertificate = true,
            cookiesFromBrowser = "firefox",
            includePresetInFilename = true,
            embedThumbnailInMp3 = true,
            parallelDownloads = 3,
            downloadDirPath = "/home/user/Downloads",
            clipboardMonitoringEnabled = true,
            autoLaunchEnabled = false,
            notifyOnComplete = true
        )

        val chromeUserState = SettingsState(
            noCheckCertificate = false,
            cookiesFromBrowser = "chrome",
            includePresetInFilename = false,
            embedThumbnailInMp3 = true,
            parallelDownloads = 5,
            downloadDirPath = "/home/user/Videos/YouTube",
            clipboardMonitoringEnabled = true,
            autoLaunchEnabled = true,
            notifyOnComplete = false
        )

        val minimalState = SettingsState(
            noCheckCertificate = false,
            cookiesFromBrowser = "",
            includePresetInFilename = true,
            embedThumbnailInMp3 = true,
            parallelDownloads = 1,
            downloadDirPath = "",
            clipboardMonitoringEnabled = false,
            autoLaunchEnabled = false,
            notifyOnComplete = true
        )

        val powerUserState = SettingsState(
            noCheckCertificate = true,
            cookiesFromBrowser = "firefox",
            includePresetInFilename = true,
            embedThumbnailInMp3 = true,
            parallelDownloads = 5,
            downloadDirPath = "/media/storage/Downloads/Videos",
            clipboardMonitoringEnabled = true,
            autoLaunchEnabled = true,
            notifyOnComplete = true
        )
    }
}
