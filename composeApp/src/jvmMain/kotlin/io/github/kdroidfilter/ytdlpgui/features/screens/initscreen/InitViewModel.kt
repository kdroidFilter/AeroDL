package io.github.kdroidfilter.ytdlpgui.features.screens.initscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.business.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.kdroidfilter.ytdlpgui.core.settings.SettingsKeys

class InitViewModel(
    private val ytDlpWrapper: YtDlpWrapper,
    private val navigator: Navigator,
    private val settings : Settings,
    // Injected to force eager initialization of clipboard monitoring
    private val clipboardMonitorManager: ClipboardMonitorManager,
    private val supportedSitesRepository: io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(InitState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
//            settings.remove(SettingsKeys.DOWNLOAD_DIR)
//            settings.remove(SettingsKeys.ONBOARDING_COMPLETED)

            // Load persisted settings for yt-dlp options
            val noCheck = settings.getBoolean(SettingsKeys.NO_CHECK_CERTIFICATE, false)
            val cookies = settings.getString(SettingsKeys.COOKIES_FROM_BROWSER, "").ifBlank { null }

            ytDlpWrapper.apply {
                noCheckCertificate = noCheck
                cookiesFromBrowser = cookies
            }.initialize { event ->
                when (event) {
                    YtDlpWrapper.InitEvent.CheckingYtDlp -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = true,
                            downloadingYtDlp = false,
                            downloadYtDlpProgress = null,
                            checkingFFmpeg = false,
                            downloadingFFmpeg = false,
                            downloadFfmpegProgress = null,
                            updatingYtdlp = false,
                            updatingFFmpeg = false,
                            errorMessage = null,
                            initCompleted = false,
                        )
                    }
                    YtDlpWrapper.InitEvent.DownloadingYtDlp -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = false,
                            downloadingYtDlp = true,
                            downloadYtDlpProgress = null,
                            errorMessage = null
                        )
                    }
                    YtDlpWrapper.InitEvent.UpdatingYtDlp -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = false,
                            updatingYtdlp = true,
                            downloadingYtDlp = false,
                            downloadYtDlpProgress = null,
                            errorMessage = null
                        )
                    }
                    YtDlpWrapper.InitEvent.EnsuringFfmpeg -> {
                        _state.value = _state.value.copy(
                            checkingFFmpeg = true,
                            downloadingFFmpeg = false,
                            downloadFfmpegProgress = null,
                            errorMessage = null
                        )
                    }
                    is YtDlpWrapper.InitEvent.YtDlpProgress -> {
                        _state.value = _state.value.copy(
                            downloadingYtDlp = true,
                            downloadYtDlpProgress = (event.percent ?: 0.0).toFloat()
                        )
                    }
                    is YtDlpWrapper.InitEvent.FfmpegProgress -> {
                        _state.value = _state.value.copy(
                            checkingFFmpeg = false,
                            downloadingFFmpeg = true,
                            downloadingYtDlp = false,
                            downloadFfmpegProgress = (event.percent ?: 0.0).toFloat()
                        )
                    }
                    is YtDlpWrapper.InitEvent.Error -> {
                        _state.value = _state.value.copy(
                            errorMessage = event.message,
                            checkingYtDlp = false,
                            checkingFFmpeg = false,
                            downloadingYtDlp = false,
                            downloadingFFmpeg = false,
                            updatingYtdlp = false,
                            updatingFFmpeg = false,
                            initCompleted = false
                        )
                    }
                    is YtDlpWrapper.InitEvent.Completed -> {
                        _state.value = _state.value.copy(
                            checkingYtDlp = false,
                            checkingFFmpeg = false,
                            downloadingYtDlp = false,
                            downloadingFFmpeg = false,
                            updatingYtdlp = false,
                            updatingFFmpeg = false,
                            initCompleted = event.success
                        )
                        viewModelScope.launch {
                            // On first initialization, fetch supported sites list from GitHub and store in DB
                            runCatching { supportedSitesRepository.initializeFromGitHubIfEmpty() }

                            // Decide whether to go through onboarding
                            val onboardingCompleted = settings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED, false)
                            val alreadyConfigured = settings.getString(SettingsKeys.DOWNLOAD_DIR, "").isNotBlank()
                            if (onboardingCompleted || alreadyConfigured) {
                                navigator.navigateAndClearBackStack(Destination.MainNavigation.Home)
                            } else {
                                navigator.navigateAndClearBackStack(Destination.Onboarding.Graph)
                            }
                        }
                    }
                }
            }
        }
    }
}