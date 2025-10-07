@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.di


import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.russhwolf.settings.Settings
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.DefaultNavigator
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import io.github.kdroidfilter.ytdlpgui.features.screens.mainnav.home.HomeViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.about.AboutViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.download.bulkdownload.BulkDownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.mainnav.downloader.DownloadViewModel
import io.github.kdroidfilter.ytdlpgui.core.business.DownloadManager
import io.github.kdroidfilter.ytdlpgui.features.screens.initscreen.InitViewModel
import io.github.kdroidfilter.ytdlpgui.core.business.ClipboardMonitorManager
import io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.settings.SettingsViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.download.singledownload.SingleDownloadViewModel
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