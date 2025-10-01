package io.github.kdroidfilter.ytdlpgui.features.screens.settings

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val navigator: Navigator,
    private val ytDlpWrapper: YtDlpWrapper,
    private val settings: Settings
) : ViewModel() {

    // Keys for persisted settings
    private val KEY_NO_CHECK_CERT = "no_check_certificate"
    private val KEY_COOKIES_FROM_BROWSER = "cookies_from_browser"
    private val KEY_INCLUDE_PRESET_IN_FILENAME = "include_preset_in_filename"
    private val KEY_PARALLEL_DOWNLOADS = "parallel_downloads"

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Backing state for options
    private val _noCheckCertificate = MutableStateFlow(settings.getBoolean(KEY_NO_CHECK_CERT, false))
    val noCheckCertificate = _noCheckCertificate.asStateFlow()

    private val _cookiesFromBrowser = MutableStateFlow(settings.getString(KEY_COOKIES_FROM_BROWSER, ""))
    val cookiesFromBrowser = _cookiesFromBrowser.asStateFlow()

    private val _includePresetInFilename = MutableStateFlow(settings.getBoolean(KEY_INCLUDE_PRESET_IN_FILENAME, true))
    val includePresetInFilename = _includePresetInFilename.asStateFlow()

    private val _parallelDownloads = MutableStateFlow(settings.getInt(KEY_PARALLEL_DOWNLOADS, 2).coerceIn(1, 10))
    val parallelDownloads = _parallelDownloads.asStateFlow()

    fun onEvents(event: SettingsEvents) {
        when (event) {
            SettingsEvents.Refresh -> {
                // Reload from persistent storage
                _noCheckCertificate.update { settings.getBoolean(KEY_NO_CHECK_CERT, false) }
                _cookiesFromBrowser.update { settings.getString(KEY_COOKIES_FROM_BROWSER, "") }
                _includePresetInFilename.update { settings.getBoolean(KEY_INCLUDE_PRESET_IN_FILENAME, true) }
                _parallelDownloads.update { settings.getInt(KEY_PARALLEL_DOWNLOADS, 2).coerceIn(1, 10) }
                // Also ensure wrapper reflects persisted values when refreshing
                ytDlpWrapper.noCheckCertificate = _noCheckCertificate.value
                ytDlpWrapper.cookiesFromBrowser = _cookiesFromBrowser.value.ifBlank { null }
            }
            is SettingsEvents.SetNoCheckCertificate -> {
                settings.putBoolean(KEY_NO_CHECK_CERT, event.enabled)
                _noCheckCertificate.value = event.enabled
                // Apply retroactively to the running wrapper instance
                ytDlpWrapper.noCheckCertificate = event.enabled
            }
            is SettingsEvents.SetCookiesFromBrowser -> {
                // Normalize value: trim, allow empty to disable
                val value = event.browser.trim()
                settings.putString(KEY_COOKIES_FROM_BROWSER, value)
                _cookiesFromBrowser.value = value
                // Apply retroactively to the running wrapper instance
                ytDlpWrapper.cookiesFromBrowser = value.ifBlank { null }
            }
            is SettingsEvents.SetIncludePresetInFilename -> {
                settings.putBoolean(KEY_INCLUDE_PRESET_IN_FILENAME, event.enabled)
                _includePresetInFilename.value = event.enabled
            }
            is SettingsEvents.SetParallelDownloads -> {
                val clamped = event.count.coerceIn(1, 10)
                settings.putInt(KEY_PARALLEL_DOWNLOADS, clamped)
                _parallelDownloads.value = clamped
            }
        }
    }
}
