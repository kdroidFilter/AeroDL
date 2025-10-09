@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.system.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val navigator: Navigator,
    private val settingsRepository: SettingsRepository,
    private val settings: Settings,
    private val trayAppState: TrayAppState,
    private val autoLaunch: AutoLaunch,
    ) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Expose settings from repository
    val noCheckCertificate: StateFlow<Boolean> = settingsRepository.noCheckCertificate
    val cookiesFromBrowser: StateFlow<String> = settingsRepository.cookiesFromBrowser
    val includePresetInFilename: StateFlow<Boolean> = settingsRepository.includePresetInFilename
    val parallelDownloads: StateFlow<Int> = settingsRepository.parallelDownloads
    val downloadDirPath: StateFlow<String> = settingsRepository.downloadDirPath
    val clipboardMonitoring: StateFlow<Boolean> = settingsRepository.clipboardMonitoringEnabled
    val notifyOnComplete: StateFlow<Boolean> = settingsRepository.notifyOnComplete

    private val _autoLaunchEnabled = MutableStateFlow(false)
    val autoLaunchEnabled = _autoLaunchEnabled.asStateFlow()

    init {
        // Query system autostart state at startup asynchronously
        viewModelScope.launch {
            val enabled = runCatching { autoLaunch.isEnabled() }.getOrDefault(false)
            _autoLaunchEnabled.value = enabled
        }
    }

    fun onEvents(event: SettingsEvents) {
        when (event) {
            SettingsEvents.Refresh -> {
                settingsRepository.refresh()
                // Refresh autostart status from system
                viewModelScope.launch {
                    val enabled = runCatching { autoLaunch.isEnabled() }.getOrElse { settings.getBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, false) }
                    _autoLaunchEnabled.value = enabled
                    settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, enabled)
                }
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
                _autoLaunchEnabled.value = event.enabled
                settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, event.enabled)
                viewModelScope.launch {
                    runCatching { if (event.enabled) autoLaunch.enable() else autoLaunch.disable() }
                    val confirmed = runCatching { autoLaunch.isEnabled() }.getOrDefault(event.enabled)
                    _autoLaunchEnabled.value = confirmed
                    settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, confirmed)
                }
            }
            is SettingsEvents.PickDownloadDir -> {
                viewModelScope.launch {
                    trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
                    val dir = FileKit.openDirectoryPicker(
                        title = event.title,
                        directory = null,
                        dialogSettings = FileKitDialogSettings()
                    )
                    dir?.let { onEvents(SettingsEvents.SetDownloadDir(it.file.absolutePath)) }
                   trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
                }
            }
        }
    }
}
