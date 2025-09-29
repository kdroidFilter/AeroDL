package io.github.kdroidfilter.ytdlpgui.di


import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.DefaultNavigator
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import org.koin.dsl.module

val appModule = module {

    single {
        YtDlpWrapper()
    }

    single<Navigator> {
        DefaultNavigator(startDestination = Destination.MainGraph)
    }


}