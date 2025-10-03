package io.github.kdroidfilter.ytdlpgui.core.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.component.TopNav
import io.github.composefluent.component.TopNavItem
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ArrowLeft
import io.github.composefluent.icons.regular.History
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Info
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
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
    val canGoBack by navigator.canGoBack.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bouton retour - visible uniquement si on peut naviguer en arrière
        if (canGoBack) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        navigator.navigateUp()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = "Retour"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // TopNav existant
        TopNav(
            expanded = expanded,
            onExpandedChanged = { expanded = it },
            modifier = Modifier.weight(1f)
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
                        coroutineScope.launch {
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
}