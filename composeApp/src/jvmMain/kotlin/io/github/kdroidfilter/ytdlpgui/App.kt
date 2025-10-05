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
import io.github.kdroidfilter.ytdlpgui.core.presentation.components.Footer
import io.github.kdroidfilter.ytdlpgui.core.presentation.components.Header
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.*
import io.github.kdroidfilter.ytdlpgui.features.screens.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.download.DownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.initscreen.InitScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingClipboardStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingCookiesStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingDownloadDirStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingFinishStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingIncludePresetStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingNoCheckStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingParallelStep
import io.github.kdroidfilter.ytdlpgui.features.screens.onboarding.OnboardingWelcomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.settings.SettingsScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.singledownload.SingleDownloadScreen
import kotlinx.coroutines.launch
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
        val currentDestination by navigator.currentDestination.collectAsState()
        if (currentDestination != Destination.InitScreen) Header(navigator = navigator)

        NavHost(
            navController = navController,
            startDestination = Destination.InitScreen,
            modifier = Modifier.fillMaxSize().weight(1f).padding(16.dp)
        ) {
            noAnimatedComposable<Destination.InitScreen> { InitScreen() }
            noAnimatedComposable<Destination.HomeScreen> { HomeScreen() }

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
            noAnimatedComposable<Destination.HistoryScreen> { DownloadScreen() }
            noAnimatedComposable<Destination.SettingsScreen> { SettingsScreen() }
            noAnimatedComposable<Destination.AboutScreen> { AboutScreen() }
        }

        if (currentDestination != Destination.InitScreen) Footer()
    }
}
