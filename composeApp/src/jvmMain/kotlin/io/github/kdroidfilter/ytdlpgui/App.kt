package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.presentation.components.AppHeader
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.*
import io.github.kdroidfilter.ytdlpgui.features.screens.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.download.DownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.initscreen.InitScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.settings.SettingsScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.singledownload.SingleDownloadScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalFluentApi::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val navigator = koinInject<Navigator>()

    // Drive NavController from Navigator events; do NOT decode routes from BackStack
    ObserveAsEvents(flow = navigator.navigationActions) { action ->
        when (action) {
            is NavigationAction.Navigate -> {
                navController.navigate(action.destination) {
                    action.navOptions(this)
                }
                // After the framework processes it, previousBackStackEntry may not be updated immediately
                // but our Navigator already updated stack/canGoBack/currentDestination.
            }
            NavigationAction.NavigateUp -> {
                // Let NavController go back; our Navigator already popped its own stack.
                navController.navigateUp()
            }
        }
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val currentDestination by navigator.currentDestination.collectAsState()
        if (currentDestination != Destination.InitScreen) AppHeader(navigator = navigator)

        NavHost(
            navController = navController,
            startDestination = Destination.InitScreen,
            modifier = Modifier.fillMaxSize()
        ) {
            noAnimatedComposable<Destination.InitScreen> { InitScreen() }
            noAnimatedComposable<Destination.HomeScreen> { HomeScreen() }
            noAnimatedComposable<Destination.BulkDownloadScreen> { BulkDownloadScreen() }
            noAnimatedComposable<Destination.SingleDownloadScreen> { SingleDownloadScreen() }
            noAnimatedComposable<Destination.HistoryScreen> { DownloadScreen() }
            noAnimatedComposable<Destination.SettingsScreen> { SettingsScreen() }
            noAnimatedComposable<Destination.AboutScreen> { AboutScreen() }
        }
    }
}
