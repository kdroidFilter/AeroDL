@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import io.github.kdroidfilter.network.CertificateValidator
import io.github.kdroidfilter.platformtools.LinuxDesktopEnvironment
import io.github.kdroidfilter.platformtools.detectLinuxDesktopEnvironment
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
    private val navigator: Navigator,
    private val trayAppState: TrayAppState,
) : ViewModel(), KoinComponent {

    private val initViewModel: InitViewModel by inject()
    val initState = initViewModel.state

    // Check if user is running GNOME desktop environment
    private val isGnome = detectLinuxDesktopEnvironment() == LinuxDesktopEnvironment.GNOME

    // Check if YouTube certificate is valid (to detect filtered networks/SSL inspection)
    private val _shouldSkipNoCheckCert = MutableStateFlow(true)
    private val shouldSkipNoCheckCert: Boolean
        get() = _shouldSkipNoCheckCert.value

    init {
        // Start downloading yt-dlp and ffmpeg in background during onboarding
        initViewModel.startInitialization(navigateToHomeWhenDone = false)
        viewModelScope.launch { settingsRepository.refreshAutoLaunchState() }

        // Check YouTube certificate validity in background
        viewModelScope.launch {
            val isValid = CertificateValidator.isYouTubeCertificateValid()
            _shouldSkipNoCheckCert.value = isValid
        }
    }

    private val _currentStep = MutableStateFlow(OnboardingStep.Welcome)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    // Expose settings from repository
    val downloadDirPath: StateFlow<String> = settingsRepository.downloadDirPath
    val cookiesFromBrowser: StateFlow<String> = settingsRepository.cookiesFromBrowser
    val includePresetInFilename: StateFlow<Boolean> = settingsRepository.includePresetInFilename
    val parallelDownloads: StateFlow<Int> = settingsRepository.parallelDownloads
    val noCheckCertificate: StateFlow<Boolean> = settingsRepository.noCheckCertificate
    val clipboardMonitoringEnabled: StateFlow<Boolean> = settingsRepository.clipboardMonitoringEnabled
    val autoLaunchEnabled: StateFlow<Boolean> = settingsRepository.autoLaunchEnabled

    private val _dependencyInfoBarDismissed = MutableStateFlow(false)
    val dependencyInfoBarDismissed: StateFlow<Boolean> = _dependencyInfoBarDismissed.asStateFlow()

    // Calculate dynamic list of steps based on environment
    private fun getEnabledSteps(): List<OnboardingStep> {
        return buildList {
            add(OnboardingStep.Welcome)
            add(OnboardingStep.DownloadDir)
            add(OnboardingStep.Cookies)
            if (!shouldSkipNoCheckCert) {
                add(OnboardingStep.NoCheckCert)
            }
            if (isGnome) {
                add(OnboardingStep.GnomeFocus)
            }
            add(OnboardingStep.Clipboard)
            add(OnboardingStep.Autostart)
            add(OnboardingStep.Finish)
        }
    }

    fun getTotalSteps(): Int = getEnabledSteps().size

    fun getCurrentStepIndex(): Int = getEnabledSteps().indexOf(_currentStep.value).coerceAtLeast(0)

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
            is OnboardingEvents.OnSetAutoLaunchEnabled -> handleSetAutoLaunchEnabled(event.enabled)
            is OnboardingEvents.OnDismissDependencyInfoBar -> handleDismissDependencyInfoBar()
        }
    }

    private fun handleStart() {
        navigateToStep(OnboardingStep.DownloadDir)
    }

    private fun handleNext() {
        val nextStep = when (_currentStep.value) {
            OnboardingStep.Welcome -> OnboardingStep.DownloadDir
            OnboardingStep.DownloadDir -> OnboardingStep.Cookies
            OnboardingStep.Cookies -> {
                if (shouldSkipNoCheckCert) {
                    if (isGnome) OnboardingStep.GnomeFocus else OnboardingStep.Clipboard
                } else {
                    OnboardingStep.NoCheckCert
                }
            }
            OnboardingStep.NoCheckCert -> if (isGnome) OnboardingStep.GnomeFocus else OnboardingStep.Clipboard
            OnboardingStep.GnomeFocus -> OnboardingStep.Clipboard
            OnboardingStep.Clipboard -> OnboardingStep.Autostart
            OnboardingStep.Autostart -> OnboardingStep.Finish
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
            OnboardingStep.GnomeFocus -> {
                if (shouldSkipNoCheckCert) OnboardingStep.Cookies else OnboardingStep.NoCheckCert
            }
            OnboardingStep.Clipboard -> {
                if (isGnome) {
                    OnboardingStep.GnomeFocus
                } else {
                    if (shouldSkipNoCheckCert) OnboardingStep.Cookies else OnboardingStep.NoCheckCert
                }
            }
            OnboardingStep.Autostart -> OnboardingStep.Clipboard
            OnboardingStep.Finish -> OnboardingStep.Autostart
        }

        navigateToStep(previousStep)
    }

    private fun handleSkip() {
        handleNext()
    }

    private fun handleFinish() {
        _currentStep.value = OnboardingStep.Finish
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            navigator.navigateAndClearBackStack(Destination.MainNavigation.Home)
        }
    }

    private fun handlePickDownloadDir(title: String) {
        viewModelScope.launch {
            trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
            val dir = FileKit.openDirectoryPicker(
                title = title,
                directory = null,
                dialogSettings = FileKitDialogSettings()
            )
            dir?.let {
                settingsRepository.setDownloadDir(it.file.absolutePath)
            }
            trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
        }
    }

    private fun handleSetCookiesFromBrowser(browser: String) {
        settingsRepository.setCookiesFromBrowser(browser)
    }

    private fun handleSetIncludePresetInFilename(include: Boolean) {
        settingsRepository.setIncludePresetInFilename(include)
    }

    private fun handleSetParallelDownloads(count: Int) {
        settingsRepository.setParallelDownloads(count)
    }

    private fun handleSetNoCheckCertificate(enabled: Boolean) {
        settingsRepository.setNoCheckCertificate(enabled)
    }

    private fun handleSetClipboardMonitoring(enabled: Boolean) {
        settingsRepository.setClipboardMonitoringEnabled(enabled)
    }

    private fun handleSetAutoLaunchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoLaunchEnabled(enabled)
        }
    }

    private fun handleDismissDependencyInfoBar() {
        _dependencyInfoBarDismissed.value = true
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
        OnboardingStep.Autostart -> Destination.Onboarding.Autostart
        OnboardingStep.Finish -> Destination.Onboarding.Finish
    }
}
