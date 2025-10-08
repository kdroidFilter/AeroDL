package io.github.kdroidfilter.ytdlpgui.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.platformtools.LinuxDesktopEnvironment
import io.github.kdroidfilter.platformtools.detectLinuxDesktopEnvironment
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.features.init.InitViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OnboardingViewModel(
    private val settings: Settings,
    private val navigator: Navigator,
) : ViewModel(), KoinComponent {

    private val initViewModel: InitViewModel by inject()
    val initState = initViewModel.state

    // Check if user is running GNOME desktop environment
    private val isGnome = detectLinuxDesktopEnvironment() == LinuxDesktopEnvironment.GNOME

    init {
        // Start downloading yt-dlp and ffmpeg in background during onboarding
        initViewModel.startInitialization(navigateToHomeWhenDone = false)
    }

    private val _currentStep = MutableStateFlow(OnboardingStep.Welcome)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _downloadDirPath = MutableStateFlow(settings.getString(SettingsKeys.DOWNLOAD_DIR, ""))
    val downloadDirPath: StateFlow<String> = _downloadDirPath.asStateFlow()

    private val _cookiesFromBrowser = MutableStateFlow(settings.getString(SettingsKeys.COOKIES_FROM_BROWSER, ""))
    val cookiesFromBrowser: StateFlow<String> = _cookiesFromBrowser.asStateFlow()

    private val _includePresetInFilename = MutableStateFlow(settings.getBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, true))
    val includePresetInFilename: StateFlow<Boolean> = _includePresetInFilename.asStateFlow()

    private val _parallelDownloads = MutableStateFlow(settings.getInt(SettingsKeys.PARALLEL_DOWNLOADS, 2))
    val parallelDownloads: StateFlow<Int> = _parallelDownloads.asStateFlow()

    private val _noCheckCertificate = MutableStateFlow(settings.getBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, false))
    val noCheckCertificate: StateFlow<Boolean> = _noCheckCertificate.asStateFlow()

    private val _clipboardMonitoringEnabled = MutableStateFlow(settings.getBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, false))
    val clipboardMonitoringEnabled: StateFlow<Boolean> = _clipboardMonitoringEnabled.asStateFlow()

    fun onEvents(event: OnboardingEvents) {
        when (event) {
            is OnboardingEvents.OnStart -> handleStart()
            is OnboardingEvents.OnNext -> handleNext()
            is OnboardingEvents.OnPrevious -> handlePrevious()
            is OnboardingEvents.OnSkip -> handleSkip()
            is OnboardingEvents.OnFinish -> handleFinish()
            is OnboardingEvents.OnPickDownloadDir -> handlePickDownloadDir(event.title)
            is OnboardingEvents.OnSetCookiesFromBrowser -> handleSetCookiesFromBrowser(event.browser)
            is OnboardingEvents.OnSetIncludePresetInFilename -> handleSetIncludePresetInFilename(event.include)
            is OnboardingEvents.OnSetParallelDownloads -> handleSetParallelDownloads(event.count)
            is OnboardingEvents.OnSetNoCheckCertificate -> handleSetNoCheckCertificate(event.enabled)
            is OnboardingEvents.OnSetClipboardMonitoring -> handleSetClipboardMonitoring(event.enabled)
        }
    }

    private fun handleStart() {
        navigateToStep(OnboardingStep.DownloadDir)
    }

    private fun handleNext() {
        val nextStep = when (_currentStep.value) {
            OnboardingStep.Welcome -> OnboardingStep.DownloadDir
            OnboardingStep.DownloadDir -> OnboardingStep.Cookies
            OnboardingStep.Cookies -> OnboardingStep.NoCheckCert
            OnboardingStep.NoCheckCert -> if (isGnome) OnboardingStep.GnomeFocus else OnboardingStep.Clipboard
            OnboardingStep.GnomeFocus -> OnboardingStep.Clipboard
            OnboardingStep.Clipboard -> OnboardingStep.Finish
            OnboardingStep.Finish -> OnboardingStep.Finish
        }

        if (nextStep == OnboardingStep.Finish && _currentStep.value == OnboardingStep.Finish) {
            handleFinish()
        } else {
            navigateToStep(nextStep)
        }
    }

    private fun handlePrevious() {
        val previousStep = when (_currentStep.value) {
            OnboardingStep.Welcome -> OnboardingStep.Welcome
            OnboardingStep.DownloadDir -> OnboardingStep.Welcome
            OnboardingStep.Cookies -> OnboardingStep.DownloadDir
            OnboardingStep.NoCheckCert -> OnboardingStep.Cookies
            OnboardingStep.GnomeFocus -> OnboardingStep.NoCheckCert
            OnboardingStep.Clipboard -> if (isGnome) OnboardingStep.GnomeFocus else OnboardingStep.NoCheckCert
            OnboardingStep.Finish -> OnboardingStep.Clipboard
        }

        navigateToStep(previousStep)
    }

    private fun handleSkip() {
        handleNext()
    }

    private fun handleFinish() {
        _currentStep.value = OnboardingStep.Finish
        viewModelScope.launch {
            settings.putBoolean(SettingsKeys.ONBOARDING_COMPLETED, true)

            // Wait for yt-dlp/ffmpeg initialization to complete before navigating to home
            initState.first { it.initCompleted }

            navigator.navigateAndClearBackStack(Destination.MainNavigation.Home)
        }
    }

    private fun handlePickDownloadDir(title: String) {
        viewModelScope.launch {
            val dir = FileKit.openDirectoryPicker(
                title = title,
                directory = null,
                dialogSettings = FileKitDialogSettings()
            )
            dir?.let {
                val path = it.file.absolutePath
                _downloadDirPath.value = path
                settings.putString(SettingsKeys.DOWNLOAD_DIR, path)
            }
        }
    }

    private fun handleSetCookiesFromBrowser(browser: String) {
        _cookiesFromBrowser.value = browser
        settings.putString(SettingsKeys.COOKIES_FROM_BROWSER, browser)
    }

    private fun handleSetIncludePresetInFilename(include: Boolean) {
        _includePresetInFilename.value = include
        settings.putBoolean(SettingsKeys.INCLUDE_PRESET_IN_FILENAME, include)
    }

    private fun handleSetParallelDownloads(count: Int) {
        _parallelDownloads.value = count
        settings.putInt(SettingsKeys.PARALLEL_DOWNLOADS, count)
    }

    private fun handleSetNoCheckCertificate(enabled: Boolean) {
        _noCheckCertificate.value = enabled
        settings.putBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, enabled)
    }

    private fun handleSetClipboardMonitoring(enabled: Boolean) {
        _clipboardMonitoringEnabled.value = enabled
        settings.putBoolean(SettingsKeys.CLIPBOARD_MONITORING_ENABLED, enabled)
    }

    private fun navigateToStep(step: OnboardingStep) {
        if (_currentStep.value == step) return
        _currentStep.value = step
        viewModelScope.launch {
            navigator.navigate(step.toDestination())
        }
    }

    private fun OnboardingStep.toDestination(): Destination.Onboarding = when (this) {
        OnboardingStep.Welcome -> Destination.Onboarding.Welcome
        OnboardingStep.DownloadDir -> Destination.Onboarding.DownloadDir
        OnboardingStep.Cookies -> Destination.Onboarding.Cookies
        OnboardingStep.NoCheckCert -> Destination.Onboarding.NoCheckCert
        OnboardingStep.GnomeFocus -> Destination.Onboarding.GnomeFocus
        OnboardingStep.Clipboard -> Destination.Onboarding.Clipboard
        OnboardingStep.Finish -> Destination.Onboarding.Finish
    }
}
