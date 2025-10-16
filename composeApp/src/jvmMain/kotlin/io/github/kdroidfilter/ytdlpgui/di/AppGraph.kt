@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.di

import coil3.ImageLoader
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.kdroidfilter.network.CoilConfig
import androidx.lifecycle.SavedStateHandle
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.NavigationEventBus
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository
import io.github.kdroidfilter.ytdlpgui.db.Database
import io.github.kdroidfilter.ytdlpgui.features.download.bulk.BulkDownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.download.manager.DownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.download.single.SingleDownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.home.HomeViewModel
import io.github.kdroidfilter.ytdlpgui.features.init.InitViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.system.about.AboutViewModel
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsViewModel
import io.github.vinceglb.autolaunch.AutoLaunch

@DependencyGraph(AppScope::class)
interface AppGraph {
    
    // Entry points for the application
    val initViewModel: InitViewModel
    val homeViewModel: HomeViewModel
    val aboutViewModel: AboutViewModel
    val onboardingViewModel: OnboardingViewModel
    val settingsViewModel: SettingsViewModel
    val bulkDownloadViewModel: BulkDownloadViewModel
    val downloadViewModel: DownloadViewModel
    val downloadManager: DownloadManager
    val clipboardMonitorManager: ClipboardMonitorManager
    
    // Additional exposed dependencies
    val autoLaunch: AutoLaunch
    val settings: Settings
    val imageLoader: ImageLoader
    val ytDlpWrapper: YtDlpWrapper
    val downloadHistoryRepository: DownloadHistoryRepository
    val navigationEventBus: NavigationEventBus

    @Provides
    @SingleIn(AppScope::class)
    fun provideYtDlpWrapper(): YtDlpWrapper = YtDlpWrapper()

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettings(): Settings = Settings()

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
        settingsRepository: SettingsRepository,
        downloadHistoryRepository: DownloadHistoryRepository,
        trayAppState: TrayAppState
    ): DownloadManager = DownloadManager(
        ytDlpWrapper,
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
    fun provideBulkDownloadViewModel(): BulkDownloadViewModel = BulkDownloadViewModel()

    @Provides
    @SingleIn(AppScope::class)
    fun provideInitViewModel(
        ytDlpWrapper: YtDlpWrapper,
        settingsRepository: SettingsRepository,
        supportedSitesRepository: SupportedSitesRepository,
        downloadHistoryRepository: DownloadHistoryRepository,
    ): InitViewModel = InitViewModel(
        ytDlpWrapper = ytDlpWrapper,
        settingsRepository = settingsRepository,
        supportedSitesRepository = supportedSitesRepository,
        downloadHistoryRepository = downloadHistoryRepository,
    )

    @Provides
    fun provideDownloadViewModel(
        downloadManager: DownloadManager,
        downloadHistoryRepository: DownloadHistoryRepository,
        initViewModel: InitViewModel
    ): DownloadViewModel = DownloadViewModel(
        downloadManager = downloadManager,
        historyRepository = downloadHistoryRepository,
        initViewModel = initViewModel
    )

    @Provides
    fun provideSingleDownloadViewModel(
        savedStateHandle: SavedStateHandle,
        ytDlpWrapper: YtDlpWrapper,
        downloadManager: DownloadManager,
    ): SingleDownloadViewModel = SingleDownloadViewModel(
        savedStateHandle = savedStateHandle,
        ytDlpWrapper = ytDlpWrapper,
        downloadManager = downloadManager,
    )

    // Convenience factory to create a route-scoped SingleDownloadViewModel from a NavBackStackEntry
    fun singleDownloadViewModel(savedStateHandle: SavedStateHandle): SingleDownloadViewModel =
        provideSingleDownloadViewModel(
            savedStateHandle = savedStateHandle,
            ytDlpWrapper = ytDlpWrapper,
            downloadManager = downloadManager,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideTrayAppState(): TrayAppState = TrayAppStateHolder.getOrCreate()

}
