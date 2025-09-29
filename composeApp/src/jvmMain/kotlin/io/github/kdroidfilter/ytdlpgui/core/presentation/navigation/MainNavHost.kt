package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.kdroidfilter.ytdlpgui.features.screens.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.history.HistoryScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.settings.SettingsScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.singledownload.SingleDownloadScreen
import org.koin.compose.koinInject

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    val navigator = koinInject<Navigator>()

    ObserveAsEvents(flow = navigator.navigationActions) { action ->
        when (action) {
            is NavigationAction.Navigate -> navController.navigate(
                action.destination
            ) {
                action.navOptions(this)
            }
            NavigationAction.NavigateUp -> navController.navigateUp()
        }
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            navigator.setCanGoBack(navController.previousBackStackEntry != null)
        }
    }

    NavHost(
        navController = navController,
        startDestination = navigator.startDestination,
        modifier = Modifier
    ) {
        navigation<Destination.MainGraph>(
            startDestination = Destination.HomeScreen
        ) {
            noAnimatedComposable<Destination.HomeScreen> { HomeScreen() }
            noAnimatedComposable<Destination.BulkDownloadScreen> { BulkDownloadScreen()}
            noAnimatedComposable<Destination.SingleDownloadScreen>{ SingleDownloadScreen() }
            noAnimatedComposable<Destination.HistoryScreen>{ HistoryScreen() }
            noAnimatedComposable<Destination.SettingsScreen> { SettingsScreen() }
            noAnimatedComposable<Destination.AboutScreen> { AboutScreen() }
        }
    }
}