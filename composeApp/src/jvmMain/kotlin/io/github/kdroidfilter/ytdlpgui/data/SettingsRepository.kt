package io.github.kdroidfilter.ytdlpgui.data

import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Single source of truth for all application settings.
 * Manages persistence, in-memory state, and synchronization with dependencies.
 */
class SettingsRepository(
    private val settings: Settings,
    private val ytDlpWrapper: YtDlpWrapper,
    private val autoLaunch: AutoLaunch,
) {
    private var clipboardMonitorManager: io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager? = null
    // StateFlows for reactive UI
    private val _noCheckCertificate = MutableStateFlow(settings.getBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, false))
    val noCheckCertificate: StateFlow<Boolean> = _noCheckCertificate.asStateFlow()

    private val _cookiesFromBrowser = MutableStateFlow(settings.getString(SettingsKeys.COOKIES_FROM_BROWSER, ""))
    val cookiesFromBrowser: StateFlow<String> = _cookiesFromBrowser.asStateFlow()

    private val _includePresetInFilename = MutableStateFlow(settings.getBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, true))
    val includePresetInFilename: StateFlow<Boolean> = _includePresetInFilename.asStateFlow()

    private val _embedThumbnailInMp3 = MutableStateFlow(settings.getBoolean(SettingsKeys.EMBED_THUMBNAIL_IN_MP3, true))
    val embedThumbnailInMp3: StateFlow<Boolean> = _embedThumbnailInMp3.asStateFlow()

    private val _parallelDownloads = MutableStateFlow(settings.getInt(SettingsKeys.PARALLEL_DOWNLOADS, 2).coerceIn(1, 10))
    val parallelDownloads: StateFlow<Int> = _parallelDownloads.asStateFlow()

    private val _downloadDirPath = MutableStateFlow(settings.getString(SettingsKeys.DOWNLOAD_DIR, ""))
    val downloadDirPath: StateFlow<String> = _downloadDirPath.asStateFlow()

    private val _clipboardMonitoringEnabled = MutableStateFlow(settings.getBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, true))
    val clipboardMonitoringEnabled: StateFlow<Boolean> = _clipboardMonitoringEnabled.asStateFlow()

    private val _notifyOnComplete = MutableStateFlow(settings.getBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, true))
    val notifyOnComplete: StateFlow<Boolean> = _notifyOnComplete.asStateFlow()

    private val _autoLaunchEnabled = MutableStateFlow(settings.getBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, false))
    val autoLaunchEnabled: StateFlow<Boolean> = _autoLaunchEnabled.asStateFlow()


    private val _concurrentFragments = MutableStateFlow(settings.getInt(SettingsKeys.CONCURRENT_FRAGMENTS, 1).coerceIn(1, 5))
    val concurrentFragments: StateFlow<Int> = _concurrentFragments.asStateFlow()

    private val _proxy = MutableStateFlow(settings.getString(SettingsKeys.PROXY, ""))
    val proxy: StateFlow<String> = _proxy.asStateFlow()

    init {
        // Apply initial settings to dependencies
        applyToYtDlpWrapper()
    }

    fun setNoCheckCertificate(enabled: Boolean) {
        _noCheckCertificate.value = enabled
        settings.putBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, enabled)
        ytDlpWrapper.noCheckCertificate = enabled
    }

    fun setCookiesFromBrowser(browser: String) {
        val value = browser.trim()
        _cookiesFromBrowser.value = value
        settings.putString(SettingsKeys.COOKIES_FROM_BROWSER, value)
        ytDlpWrapper.cookiesFromBrowser = value.ifBlank { null }
    }

    fun setIncludePresetInFilename(include: Boolean) {
        _includePresetInFilename.value = include
        settings.putBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, include)
    }

    fun setEmbedThumbnailInMp3(enabled: Boolean) {
        _embedThumbnailInMp3.value = enabled
        settings.putBoolean(SettingsKeys.EMBED_THUMBNAIL_IN_MP3, enabled)
        ytDlpWrapper.embedThumbnailInMp3 = enabled
    }

    fun setParallelDownloads(count: Int) {
        val clamped = count.coerceIn(1, 10)
        _parallelDownloads.value = clamped
        settings.putInt(SettingsKeys.PARALLEL_DOWNLOADS, clamped)
    }

    fun setDownloadDir(path: String) {
        val trimmedPath = path.trim()
        _downloadDirPath.value = trimmedPath
        settings.putString(SettingsKeys.DOWNLOAD_DIR, trimmedPath)
        ytDlpWrapper.downloadDir = trimmedPath.takeIf { it.isNotBlank() }?.let { File(it) }
    }

    fun setClipboardMonitoringEnabled(enabled: Boolean) {
        _clipboardMonitoringEnabled.value = enabled
        settings.putBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, enabled)
        clipboardMonitorManager?.onSettingChanged(enabled)
    }

    fun setNotifyOnComplete(enabled: Boolean) {
        _notifyOnComplete.value = enabled
        settings.putBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, enabled)
    }


    fun setConcurrentFragments(count: Int) {
        val clamped = count.coerceIn(1, 5)
        _concurrentFragments.value = clamped
        settings.putInt(SettingsKeys.CONCURRENT_FRAGMENTS, clamped)
        ytDlpWrapper.concurrentFragments = clamped
    }

    fun setProxy(proxyUrl: String) {
        val value = proxyUrl.trim()
        _proxy.value = value
        settings.putString(SettingsKeys.PROXY, value)
        ytDlpWrapper.proxy = value.ifBlank { null }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        settings.putBoolean(SettingsKeys.ONBOARDING_COMPLETED, completed)
    }

    fun isOnboardingCompleted(): Boolean {
        return settings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED, false)
    }

    /**
     * Refresh all values from persistent storage and apply to dependencies.
     * Useful when settings might have been modified externally.
     */
    fun refresh() {
        _noCheckCertificate.value = settings.getBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, false)
        _cookiesFromBrowser.value = settings.getString(SettingsKeys.COOKIES_FROM_BROWSER, "")
        _includePresetInFilename.value = settings.getBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, true)
        _embedThumbnailInMp3.value = settings.getBoolean(SettingsKeys.EMBED_THUMBNAIL_IN_MP3, true)
        _parallelDownloads.value = settings.getInt(SettingsKeys.PARALLEL_DOWNLOADS, 2).coerceIn(1, 10)
        _downloadDirPath.value = settings.getString(SettingsKeys.DOWNLOAD_DIR, "")
        _clipboardMonitoringEnabled.value = settings.getBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, true)
        _notifyOnComplete.value = settings.getBoolean(SettingsKeys.NOTIFY_ON_DOWNLOAD_COMPLETE, true)
        _autoLaunchEnabled.value = settings.getBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, false)
        _concurrentFragments.value = settings.getInt(SettingsKeys.CONCURRENT_FRAGMENTS, 1).coerceIn(1, 5)
        _proxy.value = settings.getString(SettingsKeys.PROXY, "")

        applyToYtDlpWrapper()
        clipboardMonitorManager?.let { applyToClipboardMonitor(it) }
    }

    suspend fun refreshAutoLaunchState() {
        val current = _autoLaunchEnabled.value
        val detected = try {
            autoLaunch.isEnabled()
        } catch (e: Exception) {
            io.github.kdroidfilter.logging.errorln { "Failed to check auto launch state: ${e.message}" }
            current
        }
        _autoLaunchEnabled.value = detected
        settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, detected)
    }

    suspend fun setAutoLaunchEnabled(enabled: Boolean) {
        _autoLaunchEnabled.value = enabled
        settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, enabled)

            if (enabled) {
                autoLaunch.enable()
            } else {
                autoLaunch.disable()
            }

        val confirmed = try {
            autoLaunch.isEnabled()
        } catch (_: Exception) {
            enabled
        }
        _autoLaunchEnabled.value = confirmed
        settings.putBoolean(SettingsKeys.AUTO_LAUNCH_ENABLED, confirmed)
    }

    private fun applyToYtDlpWrapper() {
        ytDlpWrapper.noCheckCertificate = _noCheckCertificate.value
        ytDlpWrapper.cookiesFromBrowser = _cookiesFromBrowser.value.ifBlank { null }
        val path = _downloadDirPath.value
        ytDlpWrapper.downloadDir = path.takeIf { it.isNotBlank() }?.let { File(it) }
        ytDlpWrapper.embedThumbnailInMp3 = _embedThumbnailInMp3.value
        ytDlpWrapper.concurrentFragments = _concurrentFragments.value
        ytDlpWrapper.proxy = _proxy.value.ifBlank { null }
    }

    private fun applyToClipboardMonitor(clipboardMonitor: io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager) {
        if (_clipboardMonitoringEnabled.value) {
            clipboardMonitor.onSettingChanged(true)
        }
    }

    fun setClipboardMonitorManager(manager: io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager) {
        this.clipboardMonitorManager = manager
        applyToClipboardMonitor(manager)
    }

    /**
     * Reset all settings to default values and clear persistent storage.
     * This does NOT clear binaries or download history - those are handled separately.
     */
    fun resetToDefaults() {
        settings.clear()

        // Reset in-memory state to defaults
        _noCheckCertificate.value = false
        _cookiesFromBrowser.value = ""
        _includePresetInFilename.value = true
        _embedThumbnailInMp3.value = true
        _parallelDownloads.value = 2
        _downloadDirPath.value = ""
        _clipboardMonitoringEnabled.value = true
        _notifyOnComplete.value = true
        _autoLaunchEnabled.value = false
        _concurrentFragments.value = 1
        _proxy.value = ""

        // Apply defaults to dependencies
        applyToYtDlpWrapper()
        clipboardMonitorManager?.let { applyToClipboardMonitor(it) }
    }
}
