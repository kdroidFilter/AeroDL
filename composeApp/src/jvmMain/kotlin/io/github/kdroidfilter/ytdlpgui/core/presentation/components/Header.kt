package io.github.kdroidfilter.ytdlpgui.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.MoreVertical
import io.github.composefluent.icons.regular.ArrowLeft
import io.github.composefluent.icons.regular.ArrowRight
import io.github.composefluent.icons.regular.Copy
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.History
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Info
import io.github.composefluent.icons.regular.Settings
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name
import ytdlpgui.composeapp.generated.resources.download
import ytdlpgui.composeapp.generated.resources.home

@OptIn(ExperimentalFluentApi::class)
@Composable
fun Header(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val canGoBack by navigator.canGoBack.collectAsState()
    val currentDestination by navigator.currentDestination.collectAsState()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopNav(
            expanded = expanded,
            onExpandedChanged = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            item {
                if (canGoBack) {
                    SubtleButton(
                        iconOnly = true,
                        onClick = {
                            scope.launch { navigator.navigateUp() }
                        },
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                    ) { Icon(if (isRtl) Icons.Default.ArrowRight else Icons.Default.ArrowLeft, "Back") }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 4.dp)
                    ) {
                        Icon(
                            AeroDlLogoOnly,
                            "",
                            modifier = Modifier.fillMaxHeight(0.6f),
                            tint = FluentTheme.colors.system.neutral
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.app_name),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FluentTheme.colors.system.neutral
                        )
                    }
                }
            }
            if (canGoBack) return@TopNav
            items(2) { index ->
                val (titleRes, icon, destForIndex) = when (index) {
                    0 -> Triple(Res.string.home, Icons.Default.Home, Destination.HomeScreen as Destination)
                    else -> Triple(Res.string.download, Icons.Default.History, Destination.HistoryScreen as Destination)
                }
                val isSelected = when (destForIndex) {
                    Destination.HomeScreen -> currentDestination is Destination.HomeScreen
                    Destination.HistoryScreen -> currentDestination is Destination.HistoryScreen
                    else -> false
                }
                TopNavItem(
                    selected = isSelected,
                    onClick = {
                        scope.launch {
                            navigator.navigateAndClearBackStack(destForIndex)
                        }
                    },
                    text = {
                        val full = stringResource(titleRes)
                        val display = if (full.length > 8) full.take(8) + "…" else full
                        Text(text = display)
                    },
                    icon = {
                        Icon(imageVector = icon, contentDescription = null)
                    }
                )
            }
            item {
                MenuFlyoutContainer(
                    placement = FlyoutPlacement.BottomAlignedEnd,
                    flyout = {
                        MenuFlyoutItem(
                            onClick = {
                                isFlyoutVisible = false
                                scope.launch { navigator.navigate(Destination.SettingsScreen) }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            text = { Text("Settings") }
                        )
                        MenuFlyoutItem(
                            onClick = {
                                isFlyoutVisible = false
                                scope.launch { navigator.navigate(Destination.AboutScreen) }
                            },
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            text = { Text("About") }
                        )
                    },
                    content = {
                        SubtleButton(
                            iconOnly = true,
                            onClick = { isFlyoutVisible = !isFlyoutVisible },
                            content = { Icon(Icons.Filled.MoreVertical, contentDescription = null) },
                        )
                    }
                )
            }
        }

    }
}