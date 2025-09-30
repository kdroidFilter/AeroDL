package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import kotlinx.coroutines.launch
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.about
import ytdlpgui.composeapp.generated.resources.history
import ytdlpgui.composeapp.generated.resources.home

@OptIn(ExperimentalFluentApi::class)
@Composable
fun App() {
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

    // Keep Navigator aware of back stack availability
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            navigator.setCanGoBack(navController.previousBackStackEntry != null)
        }
    }


    Column(
        Modifier.fillMaxSize().padding(4.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        val currentDestination by navigator.currentDestination.collectAsState()
        var expanded by remember { mutableStateOf(false) }
        TopNav(
            expanded = expanded,
            onExpandedChanged = { expanded = it },
        ) {
            items(3) { index ->
                val (titleRes, icon, destForIndex) = when (index) {
                    0 -> Triple(Res.string.home, Icons.Default.Home, Destination.HomeScreen as Destination)
                    1 -> Triple(Res.string.history, Icons.Default.History, Destination.HistoryScreen as Destination)
                    else -> Triple(Res.string.about, Icons.Default.Info, Destination.AboutScreen as Destination)
                }
                val isSelected = when (destForIndex) {
                    Destination.HomeScreen -> currentDestination is Destination.HomeScreen
                    Destination.HistoryScreen -> currentDestination is Destination.HistoryScreen
                    Destination.AboutScreen -> currentDestination is Destination.AboutScreen
                    else -> false
                }
                TopNavItem(
                    selected = isSelected,
                    onClick = {
                        // Drive navigation exclusively through Navigator
                        // so it becomes the single source of truth.
                        // This also works for destinations without a tab index.
                        CoroutineScope(Dispatchers.Main).launch {
                            navigator.navigate(destForIndex)
                        }
                    },
                    text = {
                        Text(text = stringResource(titleRes))
                    },
                    icon = {
                        Icon(imageVector = icon, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        NavHost(
            navController = navController,
            startDestination = navigator.startDestination,
            modifier = Modifier
        ) {
            navigation<Destination.MainGraph>(
                startDestination = currentDestination
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