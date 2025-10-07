@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.di


import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.navigation.DefaultNavigator
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
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
import io.github.vinceglb.autolaunch.AutoLaunch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { YtDlpWrapper() }
    single<Navigator> { DefaultNavigator(startDestination = Destination.InitScreen) }
    single { Settings() }

    // Database & repositories
    single { DownloadHistoryRepository.defaultDatabase() }
    single { DownloadHistoryRepository(get()) }
    single { io.github.kdroidfilter.ytdlpgui.data.SupportedSitesRepository(get()) }

    single { DownloadManager(get(), get(), get<DownloadHistoryRepository>(), get(), get()) }
    single { ClipboardMonitorManager(get(), get(), get(), get()) }
    single { InitViewModel(get(), get(), get(), get(), get())}

    // System integrations
    single { AutoLaunch(appPackageName = "io.github.kdroidfilter.ytdlpgui") }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { AboutViewModel(get()) }
    viewModel { BulkDownloadViewModel(get()) }
    viewModel { DownloadViewModel(get(), get(), get<DownloadHistoryRepository>()) }
    single { SettingsViewModel(get(), get(), get(), get(), get<TrayAppState>(), get<AutoLaunch>()) }
    viewModel { SingleDownloadViewModel(get(), get(), get(), get()) }

}