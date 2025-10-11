@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.di


import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.network.CoilConfig
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import io.github.kdroidfilter.ytdlpgui.features.home.HomeViewModel
import io.github.kdroidfilter.ytdlpgui.features.system.about.AboutViewModel
import io.github.kdroidfilter.ytdlpgui.features.download.bulk.BulkDownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.download.manager.DownloadViewModel
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.features.init.InitViewModel
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsViewModel
import io.github.kdroidfilter.ytdlpgui.features.download.single.SingleDownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.vinceglb.autolaunch.AutoLaunch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository

val appModule = module {

    single { YtDlpWrapper() }
    single { Settings() }

    // Configure Coil with native trusted roots
    single<ImageLoader> {
        CoilConfig.createImageLoader()
    }
    // System integrations
    single { AutoLaunch(appPackageName = "AeroDl") }
    // Database & repositories
    single { DownloadHistoryRepository.defaultDatabase() }
    single { DownloadHistoryRepository(get()) }
    single { SupportedSitesRepository(get()) }
    single { SettingsRepository(
        settings = get(),
        ytDlpWrapper = get(),
        clipboardMonitorManager = get(),
        autoLaunch = get()
    ) }

    single { DownloadManager({ get() }, get(), get(), get<DownloadHistoryRepository>(), get()) }
    single { ClipboardMonitorManager({ get() }, get(), get(), get()) }
    single { InitViewModel(get(), get(), get(), get(), get()) }



    viewModel { HomeViewModel(get(), get()) }
    viewModel { AboutViewModel(get(), get()) }
    viewModel { BulkDownloadViewModel(get()) }
    viewModel { DownloadViewModel(get(), get(), get<DownloadHistoryRepository>()) }
    viewModel { SettingsViewModel(navController = get(), settingsRepository = get(), trayAppState = get<TrayAppState>()) }
    viewModel { SingleDownloadViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get(), get()) }

}
