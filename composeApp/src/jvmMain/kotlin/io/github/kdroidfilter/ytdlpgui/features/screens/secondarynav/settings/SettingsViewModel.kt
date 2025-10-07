@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.business.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.settings.SettingsKeys
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val navigator: Navigator,
    private val ytDlpWrapper: YtDlpWrapper,
    private val settings: Settings,
    private val clipboardMonitorManager: ClipboardMonitorManager,
    private val trayAppState: TrayAppState,
    private val autoLaunch: AutoLaunch,
    ) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Backing state for options
    private val _noCheckCertificate = MutableStateFlow(settings.getBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, false))
    val noCheckCertificate = _noCheckCertificate.asStateFlow()

    private val _cookiesFromBrowser = MutableStateFlow(settings.getString(SettingsKeys.COOKIES_FROM_BROWSER, ""))
    val cookiesFromBrowser = _cookiesFromBrowser.asStateFlow()

    private val _includePresetInFilename = MutableStateFlow(settings.getBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, true))
    val includePresetInFilename = _includePresetInFilename.asStateFlow()

    private val _parallelDownloads = MutableStateFlow(settings.getInt(SettingsKeys.PARALLEL_DOWNLOADS, 2).coerceIn(1, 10))
    val parallelDownloads = _parallelDownloads.asStateFlow()

    private val _downloadDirPath = MutableStateFlow(settings.getString(SettingsKeys.DOWNLOAD_DIR, ""))
    val downloadDirPath = _downloadDirPath.asStateFlow()

    private val _clipboardMonitoring = MutableStateFlow(settings.getBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, false))
    val clipboardMonitoring = _clipboardMonitoring.asStateFlow()

    private val _autoLaunchEnabled = MutableStateFlow(false)
    val autoLaunchEnabled = _autoLaunchEnabled.asStateFlow()

    private val _notifyOnComplete = MutableStateFlow(settings.getBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, true))
    val notifyOnComplete = _notifyOnComplete.asStateFlow()

    init {
        // Query system autostart state at startup asynchronously
        viewModelScope.launch {
            val enabled = runCatching { autoLaunch.isEnabled() }.getOrDefault(false)
            _autoLaunchEnabled.value = enabled
        }

        // Apply persisted settings to wrapper on creation
        ytDlpWrapper.noCheckCertificate = _noCheckCertificate.value
        ytDlpWrapper.cookiesFromBrowser = _cookiesFromBrowser.value.ifBlank { null }
        val path = _downloadDirPath.value
        if (path.isNotBlank()) {
            ytDlpWrapper.downloadDir = File(path)
        }
    }

    fun onEvents(event: SettingsEvents) {
        when (event) {
            SettingsEvents.Refresh -> {
                // Reload from persistent storage
                _noCheckCertificate.update { settings.getBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, false) }
                _cookiesFromBrowser.update { settings.getString(SettingsKeys.COOKIES_FROM_BROWSER, "") }
                _includePresetInFilename.update { settings.getBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, true) }
                _parallelDownloads.update { settings.getInt(SettingsKeys.PARALLEL_DOWNLOADS, 2).coerceIn(1, 10) }
                _downloadDirPath.update { settings.getString(SettingsKeys.DOWNLOAD_DIR, "") }
                _clipboardMonitoring.update { settings.getBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, false) }
                _notifyOnComplete.update { settings.getBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, true) }
                // Refresh autostart status from system
                viewModelScope.launch {
                    val enabled = runCatching { autoLaunch.isEnabled() }.getOrElse { settings.getBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, false) }
                    _autoLaunchEnabled.value = enabled
                    settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, enabled)
                }
                // Also ensure wrapper reflects persisted values when refreshing
                ytDlpWrapper.noCheckCertificate = _noCheckCertificate.value
                ytDlpWrapper.cookiesFromBrowser = _cookiesFromBrowser.value.ifBlank { null }
                val p = _downloadDirPath.value
                ytDlpWrapper.downloadDir = p.takeIf { it.isNotBlank() }?.let { File(it) }
            }
            is SettingsEvents.SetNotifyOnComplete -> {
                settings.putBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, event.enabled)
                _notifyOnComplete.value = event.enabled
            }
            is SettingsEvents.SetNoCheckCertificate -> {
                settings.putBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, event.enabled)
                _noCheckCertificate.value = event.enabled
                // Apply retroactively to the running wrapper instance
                ytDlpWrapper.noCheckCertificate = event.enabled
            }
            is SettingsEvents.SetCookiesFromBrowser -> {
                // Normalize value: trim, allow empty to disable
                val value = event.browser.trim()
                settings.putString(SettingsKeys.COOKIES_FROM_BROWSER, value)
                _cookiesFromBrowser.value = value
                // Apply retroactively to the running wrapper instance
                ytDlpWrapper.cookiesFromBrowser = value.ifBlank { null }
            }
            is SettingsEvents.SetIncludePresetInFilename -> {
                settings.putBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, event.enabled)
                _includePresetInFilename.value = event.enabled
            }
            is SettingsEvents.SetParallelDownloads -> {
                val clamped = event.count.coerceIn(1, 10)
                settings.putInt(SettingsKeys.PARALLEL_DOWNLOADS, clamped)
                _parallelDownloads.value = clamped
            }
            is SettingsEvents.SetDownloadDir -> {
                val path = event.path.trim()
                settings.putString(SettingsKeys.DOWNLOAD_DIR, path)
                _downloadDirPath.value = path
                ytDlpWrapper.downloadDir = path.takeIf { it.isNotBlank() }?.let { File(it) }
            }
            is SettingsEvents.SetClipboardMonitoring -> {
                settings.putBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, event.enabled)
                _clipboardMonitoring.value = event.enabled
                // Persist and start/stop monitoring through the manager
                clipboardMonitorManager.onSettingChanged(event.enabled)
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
