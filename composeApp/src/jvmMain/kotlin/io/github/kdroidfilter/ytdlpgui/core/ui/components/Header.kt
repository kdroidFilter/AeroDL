@file:OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)

package io.github.kdroidfilter.ytdlpgui.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import io.github.composefluent.icons.regular.History
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Info
import io.github.composefluent.icons.regular.Settings
import io.github.kdroidfilter.ytdlpgui.core.ui.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.ui.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.app_name
import ytdlpgui.composeapp.generated.resources.download
import ytdlpgui.composeapp.generated.resources.home
import ytdlpgui.composeapp.generated.resources.tooltip_back
import ytdlpgui.composeapp.generated.resources.tooltip_home
import ytdlpgui.composeapp.generated.resources.about
import ytdlpgui.composeapp.generated.resources.settings

@OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainNavigationHeader(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier,
) {
    val currentDestination by navigator.currentDestination.collectAsState()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
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

            items(2) { index ->
                val (titleRes, icon, destForIndex) = when (index) {
                    0 -> Triple(Res.string.home, Icons.Default.Home, Destination.MainNavigation.Home as Destination)
                    else -> Triple(
                        Res.string.download,
                        Icons.Default.History,
                        Destination.MainNavigation.Downloader as Destination
                    )
                }
                val isSelected = when (destForIndex) {
                    Destination.MainNavigation.Home -> currentDestination is Destination.MainNavigation.Home
                    Destination.MainNavigation.Downloader -> currentDestination is Destination.MainNavigation.Downloader
                    else -> false
                }
                run {
                    val full = stringResource(titleRes)
                    val display = if (full.length > 8) full.take(8) + "…" else full
                    if (display != full) {
                        TooltipBox(
                            tooltip = { Text(full) }
                        ) {
                            TopNavItem(
                                selected = isSelected,
                                onClick = {
                                    scope.launch {
                                        navigator.navigateAndClearBackStack(destForIndex)
                                    }
                                },
                                text = {
                                    Text(text = display)
                                },
                                icon = {
                                    Icon(imageVector = icon, contentDescription = null)
                                }
                            )
                        }
                    } else {
                        TopNavItem(
                            selected = isSelected,
                            onClick = {
                                scope.launch {
                                    navigator.navigateAndClearBackStack(destForIndex)
                                }
                            },
                            text = {
                                Text(text = display)
                            },
                            icon = {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        )
                    }
                }
            }
            item {
                MenuFlyoutContainer(
                    placement = FlyoutPlacement.BottomAlignedEnd,
                    flyout = {
                        MenuFlyoutItem(
                            onClick = {
                                isFlyoutVisible = false
                                scope.launch { navigator.navigate(Destination.SecondaryNavigation.Settings) }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            text = { Text(stringResource(Res.string.settings)) }
                        )
                        MenuFlyoutItem(
                            onClick = {
                                isFlyoutVisible = false
                                scope.launch { navigator.navigate(Destination.SecondaryNavigation.About) }
                            },
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            text = { Text(stringResource(Res.string.about)) }
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

@Composable
fun SecondaryNavigationHeader(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier,
) {
    val previousDestination by navigator.previousDestination.collectAsState()
    val currentDestination by navigator.currentDestination.collectAsState()
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp)
    ) {
        TooltipBox(
            tooltip = { Text(stringResource(Res.string.tooltip_back)) }
        ) {
            SubtleButton(
                iconOnly = true,
                onClick = {
                    scope.launch { navigator.navigateUp() }
                },
                modifier = Modifier.padding(top = 12.dp, start = 4.dp)
            ) { Icon(if (isRtl) Icons.Default.ArrowRight else Icons.Default.ArrowLeft, "Back") }
        }
        when (currentDestination) {
            Destination.SecondaryNavigation.Settings -> {
                Text(
                    stringResource(Res.string.settings),
                    style = FluentTheme.typography.subtitle,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Destination.SecondaryNavigation.About -> {
                Text(
                    stringResource(Res.string.about),
                    style = FluentTheme.typography.subtitle,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            else -> {}
        }
        if (previousDestination !is Destination.MainNavigation.Home) {
            Spacer(Modifier.width(4.dp))
            TooltipBox(
                tooltip = { Text(stringResource(Res.string.tooltip_home)) }
            ) {
                SubtleButton(
                    iconOnly = true,
                    onClick = {
                        scope.launch { navigator.navigateAndClearBackStack(Destination.MainNavigation.Home) }
                    },
                    modifier = Modifier.padding(top = 12.dp, end = 4.dp)
                ) { Icon(Icons.Default.Home, "Home") }
            }
        } else {
            Spacer(Modifier.width(16.dp).padding(top = 12.dp))
        }
    }
}