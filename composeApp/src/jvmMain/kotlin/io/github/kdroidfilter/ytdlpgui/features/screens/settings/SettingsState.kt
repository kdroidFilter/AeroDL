package io.github.kdroidfilter.ytdlpgui.features.screens.settings

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
    val parallelDownloads: Int = 2,
    val downloadDirPath: String = "",
    val clipboardMonitoringEnabled: Boolean = false,
    val autoLaunchEnabled: Boolean = false,
    val notifyOnComplete: Boolean = true,
)

@Composable
fun collectSettingsState(viewModel: SettingsViewModel): SettingsState =
    SettingsState(
        isLoading = viewModel.isLoading.collectAsState().value,
        noCheckCertificate = viewModel.noCheckCertificate.collectAsState().value,
        cookiesFromBrowser = viewModel.cookiesFromBrowser.collectAsState().value,
        includePresetInFilename = viewModel.includePresetInFilename.collectAsState().value,
        parallelDownloads = viewModel.parallelDownloads.collectAsState().value,
        downloadDirPath = viewModel.downloadDirPath.collectAsState().value,
        clipboardMonitoringEnabled = viewModel.clipboardMonitoring.collectAsState().value,
        autoLaunchEnabled = viewModel.autoLaunchEnabled.collectAsState().value,
        notifyOnComplete = viewModel.notifyOnComplete.collectAsState().value,
    )
