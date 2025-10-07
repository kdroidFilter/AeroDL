package io.github.kdroidfilter.ytdlpgui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.ui.components.Footer
import io.github.kdroidfilter.ytdlpgui.core.ui.components.MainNavigationHeader
import io.github.kdroidfilter.ytdlpgui.core.ui.components.SecondaryNavigationHeader
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.NavigationAction
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.ObserveAsEvents
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.noAnimatedComposable
import io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.download.bulkdownload.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.mainnav.downloader.DownloaderScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.mainnav.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.initscreen.InitScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingClipboardStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingCookiesStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingDownloadDirStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingFinishStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingIncludePresetStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingNoCheckStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingParallelStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingWelcomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.secondarynav.settings.SettingsScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.download.singledownload.SingleDownloadScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
                noAnimatedComposable<Destination.Onboarding.Welcome> { OnboardingWelcomeScreen() }
                noAnimatedComposable<Destination.Onboarding.DownloadDir> { OnboardingDownloadDirStep() }
                noAnimatedComposable<Destination.Onboarding.Cookies> { OnboardingCookiesStep() }
                noAnimatedComposable<Destination.Onboarding.IncludePreset> { OnboardingIncludePresetStep() }
                noAnimatedComposable<Destination.Onboarding.Parallel> { OnboardingParallelStep() }
                noAnimatedComposable<Destination.Onboarding.NoCheckCert> { OnboardingNoCheckStep() }
                noAnimatedComposable<Destination.Onboarding.Clipboard> { OnboardingClipboardStep() }
                noAnimatedComposable<Destination.Onboarding.Finish> { OnboardingFinishStep() }
            }

            navigation<Destination.Download.Graph>(startDestination = Destination.Download.Single("")) {
                noAnimatedComposable<Destination.Download.Single> { SingleDownloadScreen() }
                noAnimatedComposable<Destination.Download.Bulk> { BulkDownloadScreen() }
            }
        }

        if (currentDestination != Destination.InitScreen) Footer()
    }
}
