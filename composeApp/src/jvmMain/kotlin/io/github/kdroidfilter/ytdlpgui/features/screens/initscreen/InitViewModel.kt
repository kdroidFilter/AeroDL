package io.github.kdroidfilter.ytdlpgui.features.screens.initscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.clipboard.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            settings.remove("download_dir")
            settings.remove("onboarding_completed")

            // Load persisted settings for yt-dlp options
            val noCheck = settings.getBoolean("no_check_certificate", false)
            val cookies = settings.getString("cookies_from_browser", "").ifBlank { null }

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
                            val onboardingCompleted = settings.getBoolean("onboarding_completed", false)
                            val alreadyConfigured = settings.getString("download_dir", "").isNotBlank()
                            if (onboardingCompleted || alreadyConfigured) {
                                navigator.navigateAndClearBackStack(Destination.HomeScreen)
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