package io.github.kdroidfilter.ytdlpgui.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.component.TopNav
import io.github.composefluent.component.TopNavItem
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.History
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Info
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.about
import ytdlpgui.composeapp.generated.resources.download
import ytdlpgui.composeapp.generated.resources.home

@OptIn(ExperimentalFluentApi::class)
@Composable
fun AppHeader(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val currentDestination by navigator.currentDestination.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    TopNav(
        expanded = expanded,
        onExpandedChanged = { expanded = it },
        modifier = modifier,
    ) {
        items(3) { index ->
            val (titleRes, icon, destForIndex) = when (index) {
                0 -> Triple(Res.string.home, Icons.Default.Home, Destination.HomeScreen as Destination)
                1 -> Triple(Res.string.download, Icons.Default.History, Destination.HistoryScreen as Destination)
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
                    // Drive navigation exclusively through Navigator so it becomes the single source of truth.
                    CoroutineScope(Dispatchers.Main).launch {
                        navigator.navigate(destForIndex)
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
}
