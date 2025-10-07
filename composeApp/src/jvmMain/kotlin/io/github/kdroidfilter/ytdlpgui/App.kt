package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.ui.Footer
import io.github.kdroidfilter.ytdlpgui.core.ui.MainNavigationHeader
import io.github.kdroidfilter.ytdlpgui.core.ui.SecondaryNavigationHeader
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.navigation.NavigationAction
import io.github.kdroidfilter.ytdlpgui.core.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.navigation.ObserveAsEvents
import io.github.kdroidfilter.ytdlpgui.core.navigation.noAnimatedComposable
import io.github.kdroidfilter.ytdlpgui.features.system.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.download.bulk.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.download.manager.DownloaderScreen
import io.github.kdroidfilter.ytdlpgui.features.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.init.InitScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.clipboard.ClipboardScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies.CookiesScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.downloaddir.DownloadDirScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.finish.FinishScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.includepreset.IncludePresetScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.nocheckcert.NoCheckCertScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.parallel.ParallelScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.welcome.WelcomeScreen
import io.github.kdroidfilter.ytdlpgui.features.system.settings.SettingsScreen
import io.github.kdroidfilter.ytdlpgui.features.download.single.SingleDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFluentApi::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val navigator = koinInject<Navigator>()
    val currentDestination by navigator.currentDestination.collectAsState()

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

    val canGoBack by navigator.canGoBack.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
            // Handle only on key-up to avoid repeats
            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Escape && canGoBack) {
                // Route back through Navigator so header state stays in sync
                scope.launch { navigator.navigateUp() }
                true // consume: prevents NavController from popping on its own
            } else {
                false
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (currentDestination is Destination.MainNavigation) MainNavigationHeader()
        if (currentDestination is Destination.SecondaryNavigation || currentDestination is Destination.Download) SecondaryNavigationHeader()

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
                noAnimatedComposable<Destination.Onboarding.IncludePreset> {
                    IncludePresetScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.Parallel> {
                    ParallelScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.NoCheckCert> {
                    NoCheckCertScreen(viewModel = onboardingViewModel(navController, it))
                }
                noAnimatedComposable<Destination.Onboarding.Clipboard> {
                    ClipboardScreen(viewModel = onboardingViewModel(navController, it))
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

        if (currentDestination != Destination.InitScreen) Footer()
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
