package io.github.kdroidfilter.ytdlpgui.di


import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.DefaultNavigator
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.state.AppViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.home.HomeViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.about.AboutViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload.BulkDownloadViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.history.HistoryViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.settings.SettingsViewModel
import io.github.kdroidfilter.ytdlpgui.features.screens.singledownload.SingleDownloadViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single {
        YtDlpWrapper()
    }

    single<Navigator> {
        DefaultNavigator(startDestination = Destination.MainGraph)
    }

    single {
        AppViewModel(get(), get())
    }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { AboutViewModel(get()) }
    viewModel { BulkDownloadViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SingleDownloadViewModel(get(), get(), get()) }

}