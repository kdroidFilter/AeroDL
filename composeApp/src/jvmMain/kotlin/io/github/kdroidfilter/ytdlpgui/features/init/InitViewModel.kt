package io.github.kdroidfilter.ytdlpgui.features.init

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.platformtools.getAppVersion
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.platformtools.releasefetcher.github.model.Asset
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.kdroidfilter.ytdlpgui.di.AppScope
import io.github.kevincianfarini.cardiologist.PulseBackpressureStrategy
import io.github.kevincianfarini.cardiologist.fixedPeriodPulse
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey(InitViewModel::class)
@Inject
class InitViewModel(
    private val ytDlpWrapper: YtDlpWrapper,
    private val ffmpegWrapper: FfmpegWrapper,
    private val settingsRepository: SettingsRepository,
    private val supportedSitesRepository: io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository,
    private val downloadHistoryRepository: io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
) : MVIViewModel<InitState, InitEvent>() {


    override fun initialState(): InitState = InitState()

    override fun handleEvent(event: InitEvent) {
        when (event) {
            InitEvent.IgnoreUpdate -> ignoreUpdate()
            InitEvent.DismissUpdateInfo -> dismissUpdateInfo()
            InitEvent.StartInitialization -> startInitialization(navigateToHomeWhenDone = true)
            InitEvent.NavigationConsumed -> {
                update { copy(navigationState = InitNavigationState.None) }
            }
        }
    }

    private var isInitializing = false

    init {
        viewModelScope.launch {
            // Check if onboarding is completed
            val onboardingCompleted = settingsRepository.isOnboardingCompleted()

            if (!onboardingCompleted) {
                // Not configured → go to onboarding
                update { copy(navigationState = InitNavigationState.NavigateToOnboarding) }
                return@launch
            }

            // Check for updates
            checkForUpdates()

            // Already configured → initialize yt-dlp and ffmpeg
            startInitialization(navigateToHomeWhenDone = true)
        }

        // Schedule periodic update checks every 12 hours (no overlap if previous run is still active)
        startPeriodicUpdateChecks()
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun startPeriodicUpdateChecks() {
        viewModelScope.launch {
            Clock.System
                .fixedPeriodPulse(12.hours)
                .beat(strategy = PulseBackpressureStrategy.SkipNext) {
                    // App update check
                    checkForUpdates()
                    // yt-dlp update check + auto-download if newer
                    runCatching { checkAndUpdateYtDlp() }
                }
        }
    }

    /**
     * Periodically checks for yt-dlp updates and downloads the latest binary if available.
     * Runs silently in background (no UI state toggling), leveraging wrapper's caching.
     */
    private suspend fun checkAndUpdateYtDlp() {
        if (ytDlpWrapper.hasUpdate()) {
            ytDlpWrapper.downloadOrUpdate()
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
                update {
                    copy(
                        updateAvailable = true,
                        latestVersion = latestVersion,
                        downloadUrl = getDownloadUrlForPlatform(latestRelease.assets),
                        releaseBody = latestRelease.body
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
     * Get the download URL - redirects to the website for all platforms
     */
    @Suppress("UNUSED_PARAMETER")
    private fun getDownloadUrlForPlatform(assets: List<Asset>): String {
        return "https://kdroidfilter.github.io/AeroDL/"
    }

    /**
     * Ignore the update notification and navigate to home
     */
    private fun ignoreUpdate() {
        update { copy(updateDismissed = true, navigationState = InitNavigationState.NavigateToHome) }
    }

    /** Persist dismissal of the update info bar for the session */
    fun dismissUpdateInfo() {
        update { copy(updateDismissed = true) }
    }

    /**
     * Start yt-dlp and ffmpeg initialization
     * @param navigateToHomeWhenDone If true, navigate to home screen when initialization is complete
     */
    fun startInitialization(navigateToHomeWhenDone: Boolean = false) {
        if (isInitializing) return
        isInitializing = true

        viewModelScope.launch {
            val noCheck = settingsRepository.noCheckCertificate.value
            val cookies = settingsRepository.cookiesFromBrowser.value.ifBlank { null }

            ytDlpWrapper.apply {
                noCheckCertificate = noCheck
                cookiesFromBrowser = cookies
            }.initialize { event ->
                when (event) {
                    YtDlpWrapper.InitEvent.CheckingYtDlp -> {
                        update {
                            copy(
                                checkingYtDlp = true,
                                downloadingYtDlp = false,
                                downloadYtDlpProgress = null,
                                checkingFFmpeg = false,
                                downloadingFFmpeg = false,
                                downloadFfmpegProgress = null,
                                checkingDeno = false,
                                downloadingDeno = false,
                                downloadDenoProgress = null,
                                updatingYtdlp = false,
                                updatingFFmpeg = false,
                                errorMessage = null,
                                initCompleted = false,
                            )
                        }
                    }
                    YtDlpWrapper.InitEvent.DownloadingYtDlp -> {
                        update {
                            copy(
                                checkingYtDlp = false,
                                downloadingYtDlp = true,
                                downloadYtDlpProgress = null,
                                errorMessage = null
                            )
                        }
                    }
                    YtDlpWrapper.InitEvent.UpdatingYtDlp -> {
                        update {
                            copy(
                                checkingYtDlp = false,
                                updatingYtdlp = true,
                                downloadingYtDlp = false,
                                downloadYtDlpProgress = null,
                                errorMessage = null
                            )
                        }
                    }
                    YtDlpWrapper.InitEvent.EnsuringFfmpeg -> {
                        update {
                            copy(
                                checkingFFmpeg = true,
                                downloadingFFmpeg = false,
                                downloadFfmpegProgress = null,
                                errorMessage = null,
                                checkingYtDlp = false,
                                downloadingYtDlp = false,
                                updatingYtdlp = false,
                            )
                        }
                    }
                    is YtDlpWrapper.InitEvent.YtDlpProgress -> {
                        update {
                            copy(
                                downloadingYtDlp = true,
                                downloadYtDlpProgress = (event.percent ?: 0.0).toFloat()
                            )
                        }
                    }
                    is YtDlpWrapper.InitEvent.FfmpegProgress -> {
                        update {
                            copy(
                                checkingFFmpeg = false,
                                downloadingFFmpeg = true,
                                downloadingYtDlp = false,
                                downloadFfmpegProgress = (event.percent ?: 0.0).toFloat()
                            )
                        }
                    }
                    YtDlpWrapper.InitEvent.EnsuringDeno -> {
                        update {
                            copy(
                                checkingDeno = true,
                                downloadingDeno = false,
                                downloadDenoProgress = null,
                                checkingFFmpeg = false,
                                downloadingFFmpeg = false,
                                errorMessage = null
                            )
                        }
                    }
                    is YtDlpWrapper.InitEvent.DenoProgress -> {
                        update {
                            copy(
                                checkingDeno = false,
                                downloadingDeno = true,
                                downloadingFFmpeg = false,
                                downloadDenoProgress = (event.percent ?: 0.0).toFloat()
                            )
                        }
                    }
                    is YtDlpWrapper.InitEvent.Error -> {
                        isInitializing = false
                        update {
                            copy(
                                errorMessage = event.message,
                                checkingYtDlp = false,
                                checkingFFmpeg = false,
                                checkingDeno = false,
                                downloadingYtDlp = false,
                                downloadingFFmpeg = false,
                                downloadingDeno = false,
                                updatingYtdlp = false,
                                updatingFFmpeg = false,
                                initCompleted = false
                            )
                        }
                    }
                    is YtDlpWrapper.InitEvent.Completed -> {
                        isInitializing = false

                        // Sync FFmpeg path from YtDlpWrapper to FfmpegWrapper
                        ytDlpWrapper.ffmpegPath?.let { path ->
                            ffmpegWrapper.ffmpegPath = path
                            // Also sync ffprobe path (same directory)
                            val ffprobeExe = if (path.endsWith(".exe")) "ffprobe.exe" else "ffprobe"
                            val ffprobePath = java.io.File(path).parentFile?.let {
                                java.io.File(it, ffprobeExe).absolutePath
                            }
                            ffprobePath?.let { ffmpegWrapper.ffprobePath = it }
                        }

                        update {
                            copy(
                                checkingYtDlp = false,
                                checkingFFmpeg = false,
                                checkingDeno = false,
                                downloadingYtDlp = false,
                                downloadingFFmpeg = false,
                                downloadingDeno = false,
                                updatingYtdlp = false,
                                updatingFFmpeg = false,
                                initCompleted = event.success
                            )
                        }
                        viewModelScope.launch {
                            // On first initialization, fetch supported sites list from GitHub and store in DB
                            runCatching { supportedSitesRepository.initializeFromGitHubIfEmpty() }

                            // Navigate to home when requested (update banner is shown later in Downloads screen)
                            if (navigateToHomeWhenDone) {
                                update { copy(navigationState = InitNavigationState.NavigateToHome) }
                            }
                        }
                    }
                }
            }
        }
    }
}
