package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.noAnimatedComposable
import io.github.kdroidfilter.ytdlpgui.core.ui.Footer
import io.github.kdroidfilter.ytdlpgui.core.ui.MainNavigationHeader
import io.github.kdroidfilter.ytdlpgui.core.ui.SecondaryNavigationHeader
import io.github.kdroidfilter.ytdlpgui.features.download.bulk.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.download.manager.DownloaderScreen
import io.github.kdroidfilter.ytdlpgui.features.download.single.SingleDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.init.InitScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.autostart.AutostartScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.clipboard.ClipboardScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies.CookiesScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.downloaddir.DownloadDirScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.finish.FinishScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.gnomefocus.GnomeFocusScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.nocheckcert.NoCheckCertScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.welcome.WelcomeScreen
import io.github.kdroidfilter.ytdlpgui.features.system.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFluentApi::class)
@Composable
fun App() {
    // Inject NavController from Koin (registered in main.kt, same pattern as TrayAppState)
    val navController = koinInject<NavHostController>()

    // Observe current back stack entry to determine which header to show
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Determine current destination type using type-safe hasRoute()
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
    val isInitScreen = currentDestination?.hasRoute(Destination.InitScreen::class) == true

    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (isMainNavigation) MainNavigationHeader()
        if (isSecondaryNavigation || isDownload) SecondaryNavigationHeader()

        NavHost(
            navController = navController,
            startDestination = Destination.InitScreen,
            modifier = Modifier.fillMaxSize().weight(1f).padding(start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            noAnimatedComposable<Destination.InitScreen> { InitScreen() }

            navigation<Destination.MainNavigation.Graph>(startDestination = Destination.MainNavigation.Home) {
                noAnimatedComposable<Destination.MainNavigation.Home> { HomeScreen() }
                noAnimatedComposable<Destination.MainNavigation.Downloader> { DownloaderScreen() }
            }

            navigation<Destination.SecondaryNavigation.Graph>(startDestination = Destination.SecondaryNavigation.Settings) {
                noAnimatedComposable<Destination.SecondaryNavigation.Settings> { SettingsScreen() }
                noAnimatedComposable<Destination.SecondaryNavigation.About> { AboutScreen() }
            }

            navigation<Destination.Onboarding.Graph>(startDestination = Destination.Onboarding.Welcome) {
                noAnimatedComposable<Destination.Onboarding.Welcome> {
                    WelcomeScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.DownloadDir> {
                    DownloadDirScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.Cookies> {
                    CookiesScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.NoCheckCert> {
                    NoCheckCertScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.GnomeFocus> {
                    GnomeFocusScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.Clipboard> {
                    ClipboardScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.Autostart> {
                    AutostartScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.Finish> {
                    FinishScreen(viewModel = onboardingViewModel(navController, it))
                }
            }

            navigation<Destination.Download.Graph>(startDestination = Destination.Download.Single("")) {
                noAnimatedComposable<Destination.Download.Single> { SingleDownloadScreen() }
                noAnimatedComposable<Destination.Download.Bulk> { BulkDownloadScreen() }
            }
        }

        if (!isInitScreen) Footer()
    }
}

@Composable
private fun onboardingViewModel(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry
): OnboardingViewModel {
    val parentEntry = remember(backStackEntry) {
        backStackEntry.destination.parent?.route?.let(navController::getBackStackEntry) ?: backStackEntry
    }
    return koinViewModel(viewModelStoreOwner = parentEntry)
}
