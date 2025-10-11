package io.github.kdroidfilter.ytdlpgui.features.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.config.client
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.platformtools.releasefetcher.github.model.Asset

class InitViewModel(
    private val ytDlpWrapper: YtDlpWrapper,
    private val navController: NavHostController,
    private val settings : Settings,
    // Injected to force eager initialization of clipboard monitoring
    private val clipboardMonitorManager: ClipboardMonitorManager,
    private val supportedSitesRepository: io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(InitState())
    val state = _state.asStateFlow()

    private var isInitializing = false

    init {
        viewModelScope.launch {
            // Check if onboarding is completed
            val onboardingCompleted = settings.getBoolean(SettingsKeys.ONBOARDING_COMPLETED, false)

            if (!onboardingCompleted) {
                // Not configured → go to onboarding
                navController.navigate(Destination.Onboarding.Graph) {
                    popUpTo(Destination.InitScreen) { inclusive = true }
                    launchSingleTop = true
                }
                return@launch
            }

            // Check for updates
            checkForUpdates()

            // Already configured → initialize yt-dlp and ffmpeg
            startInitialization(navigateToHomeWhenDone = true)
        }
    }

    /**
     * Check if a new version is available on GitHub
     */
    private suspend fun checkForUpdates() {
        runCatching {
            val currentVersion = getAppVersion()
            val fetcher = GitHubReleaseFetcher(
                owner = "kdroidFilter",
                repo = "AeroDl",
                KtorConfig.createHttpClient()
            )

            val latestRelease = fetcher.getLatestRelease() ?: return@runCatching
            val latestVersion = latestRelease.tag_name.removePrefix("v")

            // Compare versions
            if (isNewerVersion(currentVersion, latestVersion)) {
                // Determine the appropriate download URL based on OS
                val downloadUrl = getDownloadUrlForPlatform(latestRelease.assets)

                // Only show update if there's an asset available for this platform
                if (downloadUrl != null) {
                    _state.value = _state.value.copy(
                        updateAvailable = true,
                        latestVersion = latestVersion,
                        downloadUrl = downloadUrl
                    )
                }
            }
        }
    }

    /**
     * Compare two version strings (e.g., "1.0.0" vs "1.0.1")
     * Returns true if newVersion is newer than currentVersion
     */
    private fun isNewerVersion(currentVersion: String, newVersion: String): Boolean {
        val current = currentVersion.split(".").mapNotNull { it.toIntOrNull() }
        val new = newVersion.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(current.size, new.size)) {
            val currentPart = current.getOrNull(i) ?: 0
            val newPart = new.getOrNull(i) ?: 0

            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }

        return false
    }

    /**
     * Get the appropriate download URL for the current platform
     */
    private fun getDownloadUrlForPlatform(assets: List<Asset>): String? {
        val os = getOperatingSystem()
        val extension = when (os) {
            OperatingSystem.WINDOWS -> ".msi"
            OperatingSystem.MACOS -> ".pkg"
            OperatingSystem.LINUX -> ".deb"
            else -> null
        }

        return extension?.let { ext ->
            assets.firstOrNull { it.name.endsWith(ext, ignoreCase = true) }?.browser_download_url
        }
    }

    /**
     * Ignore the update notification and navigate to home
     */
    fun ignoreUpdate() {
        navController.navigate(Destination.MainNavigation.Home) {
            popUpTo(Destination.InitScreen) { inclusive = true }
            launchSingleTop = true
        }
    }

    /**
     * Start yt-dlp and ffmpeg initialization
     * @param navigateToHomeWhenDone If true, navigate to home screen when initialization is complete
     */
    fun startInitialization(navigateToHomeWhenDone: Boolean = false) {
        if (isInitializing) return
        isInitializing = true

        viewModelScope.launch {
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
                            errorMessage = null,
                            checkingYtDlp = false,
                            downloadingYtDlp = false,
                            updatingYtdlp = false,

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

                            // Navigate to home only if requested and no update is available
                            if (navigateToHomeWhenDone && !_state.value.updateAvailable) {
                                navController.navigate(Destination.MainNavigation.Home) {
                                    popUpTo(Destination.InitScreen) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
