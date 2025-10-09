package io.github.kdroidfilter.ytdlpgui.features.system.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

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

@Composable
fun collectSettingsState(viewModel: SettingsViewModel): SettingsState =
    SettingsState(
        isLoading = viewModel.isLoading.collectAsState().value,
        noCheckCertificate = viewModel.noCheckCertificate.collectAsState().value,
        cookiesFromBrowser = viewModel.cookiesFromBrowser.collectAsState().value,
        includePresetInFilename = viewModel.includePresetInFilename.collectAsState().value,
        embedThumbnailInMp3 = viewModel.embedThumbnailInMp3.collectAsState().value,
        parallelDownloads = viewModel.parallelDownloads.collectAsState().value,
        downloadDirPath = viewModel.downloadDirPath.collectAsState().value,
        clipboardMonitoringEnabled = viewModel.clipboardMonitoring.collectAsState().value,
        autoLaunchEnabled = viewModel.autoLaunchEnabled.collectAsState().value,
        notifyOnComplete = viewModel.notifyOnComplete.collectAsState().value,
    )
