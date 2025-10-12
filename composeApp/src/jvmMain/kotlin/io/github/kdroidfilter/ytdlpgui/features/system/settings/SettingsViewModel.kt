@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.system.settings

import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import io.github.kdroidfilter.platformtools.appmanager.restartApplication
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val navController: NavHostController,
    private val settingsRepository: SettingsRepository,
    private val downloadHistoryRepository: DownloadHistoryRepository,
    private val trayAppState: TrayAppState,
) : MVIViewModel<SettingsState, SettingsEvents>() {

    override fun initialState(): SettingsState = SettingsState.defaultState

    private val _isLoading = MutableStateFlow(false)

    // Expose settings from repository
    val noCheckCertificate: StateFlow<Boolean> = settingsRepository.noCheckCertificate
    val cookiesFromBrowser: StateFlow<String> = settingsRepository.cookiesFromBrowser
    val includePresetInFilename: StateFlow<Boolean> = settingsRepository.includePresetInFilename
    val embedThumbnailInMp3: StateFlow<Boolean> = settingsRepository.embedThumbnailInMp3
    val parallelDownloads: StateFlow<Int> = settingsRepository.parallelDownloads
    val downloadDirPath: StateFlow<String> = settingsRepository.downloadDirPath
    val clipboardMonitoring: StateFlow<Boolean> = settingsRepository.clipboardMonitoringEnabled
    val notifyOnComplete: StateFlow<Boolean> = settingsRepository.notifyOnComplete
    val autoLaunchEnabled: StateFlow<Boolean> = settingsRepository.autoLaunchEnabled

    // Note: This ViewModel uses a combined state from multiple sources, so we override uiState
    override val uiState = combine(
        _isLoading,
        noCheckCertificate,
        cookiesFromBrowser,
        includePresetInFilename,
        embedThumbnailInMp3,
        parallelDownloads,
        downloadDirPath,
        clipboardMonitoring,
        autoLaunchEnabled,
        notifyOnComplete,
    ) { values: Array<Any?> ->
        val loading = values[0] as Boolean
        val noCheck = values[1] as Boolean
        val cookies = values[2] as String
        val includePreset = values[3] as Boolean
        val embedThumb = values[4] as Boolean
        val parallel = values[5] as Int
        val downloadDir = values[6] as String
        val clipboard = values[7] as Boolean
        val autoLaunch = values[8] as Boolean
        val notify = values[9] as Boolean
        SettingsState(
            isLoading = loading,
            noCheckCertificate = noCheck,
            cookiesFromBrowser = cookies,
            includePresetInFilename = includePreset,
            embedThumbnailInMp3 = embedThumb,
            parallelDownloads = parallel,
            downloadDirPath = downloadDir,
            clipboardMonitoringEnabled = clipboard,
            autoLaunchEnabled = autoLaunch,
            notifyOnComplete = notify,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState.defaultState,
    )

    init {
        // Query system autostart state at startup asynchronously
        viewModelScope.launch { settingsRepository.refreshAutoLaunchState() }
    }

    override fun handleEvent(event: SettingsEvents) {
        when (event) {
            SettingsEvents.Refresh -> {
                settingsRepository.refresh()
                // Refresh autostart status from system
                viewModelScope.launch { settingsRepository.refreshAutoLaunchState() }
            }
            is SettingsEvents.SetNotifyOnComplete -> {
                settingsRepository.setNotifyOnComplete(event.enabled)
            }
            is SettingsEvents.SetNoCheckCertificate -> {
                settingsRepository.setNoCheckCertificate(event.enabled)
            }
            is SettingsEvents.SetCookiesFromBrowser -> {
                settingsRepository.setCookiesFromBrowser(event.browser)
            }
            is SettingsEvents.SetIncludePresetInFilename -> {
                settingsRepository.setIncludePresetInFilename(event.enabled)
            }
            is SettingsEvents.SetEmbedThumbnailInMp3 -> {
                settingsRepository.setEmbedThumbnailInMp3(event.enabled)
            }
            is SettingsEvents.SetParallelDownloads -> {
                settingsRepository.setParallelDownloads(event.count)
            }
            is SettingsEvents.SetDownloadDir -> {
                settingsRepository.setDownloadDir(event.path)
            }
            is SettingsEvents.SetClipboardMonitoring -> {
                settingsRepository.setClipboardMonitoringEnabled(event.enabled)
            }
            is SettingsEvents.SetAutoLaunchEnabled -> {
                viewModelScope.launch { settingsRepository.setAutoLaunchEnabled(event.enabled) }
            }
            is SettingsEvents.PickDownloadDir -> {
                viewModelScope.launch {
                    trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
                    val dir = FileKit.openDirectoryPicker(
                        title = event.title,
                        directory = null,
                        dialogSettings = FileKitDialogSettings()
                    )
                    dir?.let { handleEvent(SettingsEvents.SetDownloadDir(it.file.absolutePath)) }
                   trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
                }
            }
            SettingsEvents.ResetToDefaults -> {
                viewModelScope.launch {
                    // 1. Clear settings
                    settingsRepository.resetToDefaults()

                    // 2. Clear download history
                    downloadHistoryRepository.clear()

                    // 3. Delete yt-dlp/FFmpeg binaries and temp files
                    clearBinariesAndTemp()

                    // 4. Refresh autostart state
                    settingsRepository.refreshAutoLaunchState()

                    // 5. Restart the app
                    restartApplication()
                }
            }
        }
    }

    private fun clearBinariesAndTemp() {
        // Clear java temp directory
        val tmpDir = System.getProperty("java.io.tmpdir")
        try {
            File(tmpDir).listFiles()?.forEach { file ->
                try {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                } catch (_: Exception) { /* ignore */ }
            }
        } catch (_: Exception) { /* ignore */ }
    }
}
