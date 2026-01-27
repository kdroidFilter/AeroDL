@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.di

import coil3.ImageLoader
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import io.github.kdroidfilter.network.CoilConfig
import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.NavigationEventBus
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository
import io.github.kdroidfilter.ytdlpgui.db.Database
import io.github.vinceglb.autolaunch.AutoLaunch

@DependencyGraph(AppScope::class)
abstract class AppGraph : ViewModelGraph {

    // Managers
    abstract val downloadManager: DownloadManager
    abstract val clipboardMonitorManager: ClipboardMonitorManager

    // Core dependencies
    abstract val autoLaunch: AutoLaunch
    abstract val settings: Settings
    abstract val imageLoader: ImageLoader
    abstract val ytDlpWrapper: YtDlpWrapper
    abstract val ffmpegWrapper: FfmpegWrapper
    abstract val downloadHistoryRepository: DownloadHistoryRepository
    abstract val navigationEventBus: NavigationEventBus

    // Factory for SingleDownloadViewModel (needs SavedStateHandle via assisted injection)
    abstract val singleDownloadViewModelFactory: io.github.kdroidfilter.ytdlpgui.features.download.single.SingleDownloadViewModel.Factory

    // Factory for BulkDownloadViewModel (needs SavedStateHandle via assisted injection)
    abstract val bulkDownloadViewModelFactory: io.github.kdroidfilter.ytdlpgui.features.download.bulk.BulkDownloadViewModel.Factory

    // Factory for ConverterOptionsViewModel (needs SavedStateHandle via assisted injection)
    abstract val converterOptionsViewModelFactory: io.github.kdroidfilter.ytdlpgui.features.converter.ConverterOptionsViewModel.Factory

    @Provides
    @SingleIn(AppScope::class)
    fun provideYtDlpWrapper(): YtDlpWrapper = YtDlpWrapper()

    @Provides
    @SingleIn(AppScope::class)
    fun provideFfmpegWrapper(): FfmpegWrapper = FfmpegWrapper()

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettings(): Settings {
        val prefs = Preferences.userRoot().node("io/github/kdroidfilter/aerodl")
        return PreferencesSettings(prefs)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideImageLoader(): ImageLoader = CoilConfig.createImageLoader()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAutoLaunch(): AutoLaunch = AutoLaunch(appPackageName = "AeroDl")

    @Provides
    @SingleIn(AppScope::class)
    fun provideNavigationEventBus(): NavigationEventBus = NavigationEventBus()

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(): Database =
        DownloadHistoryRepository.defaultDatabase()

    @Provides
    @SingleIn(AppScope::class)
    fun provideDownloadHistoryRepository(
        database: Database
    ): DownloadHistoryRepository = DownloadHistoryRepository(database)

    @Provides
    @SingleIn(AppScope::class)
    fun provideSupportedSitesRepository(
        database: Database
    ): SupportedSitesRepository = SupportedSitesRepository(database)

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsRepository(
        settings: Settings,
        ytDlpWrapper: YtDlpWrapper,
        autoLaunch: AutoLaunch
    ): SettingsRepository = SettingsRepository(
        settings = settings,
        ytDlpWrapper = ytDlpWrapper,
        autoLaunch = autoLaunch
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideDownloadManager(
        ytDlpWrapper: YtDlpWrapper,
        ffmpegWrapper: FfmpegWrapper,
        settingsRepository: SettingsRepository,
        downloadHistoryRepository: DownloadHistoryRepository,
        trayAppState: TrayAppState
    ): DownloadManager = DownloadManager(
        ytDlpWrapper,
        ffmpegWrapper,
        settingsRepository,
        downloadHistoryRepository,
        trayAppState
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideClipboardMonitorManager(
        settingsRepository: SettingsRepository,
        trayAppState: TrayAppState,
        supportedSitesRepository: SupportedSitesRepository,
        navigationEventBus: NavigationEventBus,
        ytDlpWrapper: YtDlpWrapper,
    ): ClipboardMonitorManager {
        val manager = ClipboardMonitorManager(
            settingsRepository,
            trayAppState,
            supportedSitesRepository,
            navigationEventBus,
            ytDlpWrapper,
        )
        // Connect SettingsRepository to ClipboardMonitorManager after creation
        settingsRepository.setClipboardMonitorManager(manager)
        return manager
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideTrayAppState(): TrayAppState = TrayAppStateHolder.getOrCreate()

}
