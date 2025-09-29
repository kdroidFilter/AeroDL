package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.component.TopNav
import io.github.composefluent.component.TopNavItem
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.History
import io.github.composefluent.icons.regular.Info
import io.github.kdroidfilter.ytdlpgui.features.screens.about.AboutScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload.BulkDownloadScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.history.HistoryScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.home.HomeScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.settings.SettingsScreen
import io.github.kdroidfilter.ytdlpgui.features.screens.singledownload.SingleDownloadScreen
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.about
import ytdlpgui.composeapp.generated.resources.history
import ytdlpgui.composeapp.generated.resources.home

@OptIn(ExperimentalFluentApi::class)
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
    Column(
        Modifier.fillMaxSize().padding(4.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        var selectedIndex by remember { mutableStateOf(0) }
        var expanded by remember { mutableStateOf(false) }
        TopNav(
            expanded = expanded,
            onExpandedChanged = { expanded = it },
        ) {
            items(3) { index ->
                val (titleRes, icon) = when (index) {
                    0 -> Res.string.home to Icons.Default.Home
                    1 -> Res.string.history to Icons.Default.History
                    else -> Res.string.about to Icons.Default.Info
                }
                TopNavItem(
                    selected = index == selectedIndex,
                    onClick = {
                        selectedIndex = index
                        when (index) {
                            0 -> navController.navigate(Destination.HomeScreen)
                            1 -> navController.navigate(Destination.HistoryScreen)
                            else -> navController.navigate(Destination.AboutScreen)
                        }
                    },
                    text = {
                        Text(text = stringResource(titleRes))
                    },
                    icon = {
                        Icon(imageVector = icon, contentDescription = null)
                    }
                )
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
                noAnimatedComposable<Destination.BulkDownloadScreen> { BulkDownloadScreen() }
                noAnimatedComposable<Destination.SingleDownloadScreen> { SingleDownloadScreen() }
                noAnimatedComposable<Destination.HistoryScreen> { HistoryScreen() }
                noAnimatedComposable<Destination.SettingsScreen> { SettingsScreen() }
                noAnimatedComposable<Destination.AboutScreen> { AboutScreen() }
            }
        }
    }
}