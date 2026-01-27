package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.noAnimatedComposable
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import io.github.kdroidfilter.ytdlpgui.core.ui.Footer
import io.github.kdroidfilter.ytdlpgui.core.ui.MainNavigationHeader
import io.github.kdroidfilter.ytdlpgui.core.ui.SecondaryNavigationHeader
import io.github.kdroidfilter.ytdlpgui.features.download.bulk.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.download.manager.DownloaderScreen
import io.github.kdroidfilter.ytdlpgui.features.download.single.SingleDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.init.InitScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.autostart.AutostartScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.clipboard.ClipboardScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies.CookiesScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.downloaddir.DownloadDirScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.finish.FinishScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.gnomefocus.GnomeFocusScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.nocheckcert.NoCheckCertScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.welcome.WelcomeScreen
import io.github.kdroidfilter.ytdlpgui.features.converter.ConverterInputScreen
import io.github.kdroidfilter.ytdlpgui.features.converter.ConverterOptionsScreen
import io.github.kdroidfilter.ytdlpgui.features.system.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsScreen

@OptIn(ExperimentalFluentApi::class)
@Composable
fun App() {
    val navController = rememberNavController()

    // Bridge: observe DI-backed navigation events and apply to NavController
    val appGraph = LocalAppGraph.current
    LaunchedEffect(navController) {
        appGraph.navigationEventBus.events.collect { destination ->
            runCatching { navController.navigate(destination) }
        }
    }


    // Observe current back stack entry to determine which header to show
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isMainNavigation = currentDestination?.hierarchy?.any {
        it.hasRoute(Destination.MainNavigation.Graph::class) ||
                it.hasRoute(Destination.MainNavigation.Home::class) ||
                it.hasRoute(Destination.MainNavigation.Downloader::class)
    } == true
    val isSecondaryNavigation = currentDestination?.hierarchy?.any {
        it.hasRoute(Destination.SecondaryNavigation.Graph::class) ||
                it.hasRoute(Destination.SecondaryNavigation.Settings::class) ||
                it.hasRoute(Destination.SecondaryNavigation.About::class)
    } == true
    val isDownload = currentDestination?.hierarchy?.any {
        it.hasRoute(Destination.Download.Graph::class) ||
                it.hasRoute(Destination.Download.Single::class) ||
                it.hasRoute(Destination.Download.Bulk::class)
    } == true
    val isConverter = currentDestination?.hierarchy?.any {
        it.hasRoute(Destination.Converter.Graph::class) ||
                it.hasRoute(Destination.Converter.Input::class) ||
                it.hasRoute(Destination.Converter.Options::class)
    } == true
    val isInitScreen = currentDestination?.hasRoute(Destination.InitScreen::class) == true

    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (isMainNavigation) MainNavigationHeader(navController)
        if (isSecondaryNavigation || isDownload || isConverter) SecondaryNavigationHeader(navController)

        NavHost(
            navController = navController,
            startDestination = Destination.InitScreen,
            modifier = Modifier.fillMaxSize().weight(1f).padding(start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            noAnimatedComposable<Destination.InitScreen> { InitScreen(navController) }

            navigation<Destination.MainNavigation.Graph>(startDestination = Destination.MainNavigation.Home) {
                noAnimatedComposable<Destination.MainNavigation.Home> { HomeScreen(navController) }
                noAnimatedComposable<Destination.MainNavigation.Downloader> { DownloaderScreen() }
            }

            navigation<Destination.SecondaryNavigation.Graph>(startDestination = Destination.SecondaryNavigation.Settings) {
                noAnimatedComposable<Destination.SecondaryNavigation.Settings> { SettingsScreen() }
                noAnimatedComposable<Destination.SecondaryNavigation.About> { AboutScreen() }
            }

            navigation<Destination.Onboarding.Graph>(startDestination = Destination.Onboarding.Welcome) {
                noAnimatedComposable<Destination.Onboarding.Welcome> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { WelcomeScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.DownloadDir> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { DownloadDirScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.Cookies> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { CookiesScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.NoCheckCert> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { NoCheckCertScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.GnomeFocus> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { GnomeFocusScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.Clipboard> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { ClipboardScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.Autostart> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { AutostartScreen(navController) }
                }
                noAnimatedComposable<Destination.Onboarding.Finish> { backStackEntry ->
                    WithParentViewModelStoreOwner(navController, backStackEntry) { FinishScreen(navController) }
                }
            }

            navigation<Destination.Download.Graph>(startDestination = Destination.Download.Single("")) {
                noAnimatedComposable<Destination.Download.Single> { backStackEntry -> SingleDownloadScreen(navController, backStackEntry) }
                noAnimatedComposable<Destination.Download.Bulk> { backStackEntry -> BulkDownloadScreen(navController, backStackEntry) }
            }

            navigation<Destination.Converter.Graph>(startDestination = Destination.Converter.Input) {
                noAnimatedComposable<Destination.Converter.Input> { ConverterInputScreen(navController) }
                noAnimatedComposable<Destination.Converter.Options> { backStackEntry -> ConverterOptionsScreen(navController, backStackEntry) }
            }
        }

        if (!isInitScreen) Footer()
    }
}

@Composable
private fun WithParentViewModelStoreOwner(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    content: @Composable () -> Unit,
) {
    val parentEntry = remember(backStackEntry) {
        backStackEntry.destination.parent?.route?.let(navController::getBackStackEntry) ?: backStackEntry
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides parentEntry) {
        content()
    }
}
