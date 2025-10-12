@file:OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)

package io.github.kdroidfilter.ytdlpgui.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.MoreVertical
import io.github.composefluent.icons.regular.*
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.features.init.InitViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ytdlpgui.composeapp.generated.resources.*

@OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainNavigationHeader(
    navController: NavHostController = koinInject(),
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // App update badge state (from InitViewModel)
    val initViewModel = koinInject<InitViewModel>()
    val initState = initViewModel.uiState.collectAsState().value
    val showUpdateBadge = initState.updateAvailable && !initState.updateDismissed

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
                val showBadge = (index == 1) && showUpdateBadge
                val isSelected = currentDestination?.hierarchy?.any {
                    it.hasRoute(destForIndex::class)
                } == true
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
                                    navController.navigate(destForIndex) {
                                        popUpTo(Destination.InitScreen) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                text = {
                                    Text(text = display)
                                },
                                icon = {
                                    Icon(imageVector = icon, contentDescription = null)
                                },
                                badge = if (showBadge) ({ UpdateNavBadge() }) else null,
                            )
                        }
                    } else {
                        TopNavItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destForIndex) {
                                    popUpTo(Destination.InitScreen) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            text = {
                                Text(text = display)
                            },
                            icon = {
                                Icon(imageVector = icon, contentDescription = null)
                            },
                            badge = if (showBadge) ({ UpdateNavBadge() }) else null,
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
                                navController.navigate(Destination.SecondaryNavigation.Settings)
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            text = { Text(stringResource(Res.string.settings)) }
                        )
                        MenuFlyoutItem(
                            onClick = {
                                isFlyoutVisible = false
                                navController.navigate(Destination.SecondaryNavigation.About)
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
private fun CenterHeaderBar(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    actions: (@Composable () -> Unit)?
) {
    // Custom layout: title is measured after knowing side widths,
    // centered when possible, and shifted just enough to avoid overlap.
    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        val navPlaceables = navigationIcon
            ?.let { subcompose("nav", it).map { it.measure(loose) } }
            ?: emptyList()
        val actionsPlaceables = actions
            ?.let { subcompose("actions", it).map { it.measure(loose) } }
            ?: emptyList()

        val navWidth = navPlaceables.maxOfOrNull { it.width } ?: 0
        val actionsWidth = actionsPlaceables.maxOfOrNull { it.width } ?: 0

        val titleMaxWidth = (constraints.maxWidth - navWidth - actionsWidth).coerceAtLeast(0)
        val titlePlaceables = subcompose("title", title).map {
            it.measure(loose.copy(maxWidth = titleMaxWidth))
        }

        val width = constraints.maxWidth
        val height = maxOf(
            constraints.minHeight,
            navPlaceables.maxOfOrNull { it.height } ?: 0,
            actionsPlaceables.maxOfOrNull { it.height } ?: 0,
            titlePlaceables.maxOfOrNull { it.height } ?: 0
        )

        layout(width, height) {
            // Left (nav)
            var x = 0
            navPlaceables.forEach {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width
            }

            // Right (actions)
            var rightX = width
            actionsPlaceables.forEach { p ->
                rightX -= p.width
                p.placeRelative(rightX, (height - p.height) / 2)
            }

            // Title (centered, but clamped to avoid overlap with sides)
            val tW = titlePlaceables.maxOfOrNull { it.width } ?: 0
            var titleX = (width - tW) / 2
            if (titleX < navWidth) titleX = navWidth
            if (titleX + tW > width - actionsWidth) titleX = (width - actionsWidth) - tW

            titlePlaceables.forEach {
                it.placeRelative(titleX, (height - it.height) / 2)
            }
        }
    }
}

@Composable
private fun UpdateNavBadge() {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Badge(
            status = BadgeStatus.Critical,
            content = { BadgeDefaults.Icon(status = BadgeStatus.Informational) },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
fun SecondaryNavigationHeader(
    navController: NavHostController = koinInject(),
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Check if previous destination is Home using type-safe hasRoute()
    val previousEntry = navController.previousBackStackEntry
    val isPreviousHome = previousEntry?.destination?.hierarchy?.any {
        it.hasRoute(Destination.MainNavigation.Home::class)
    } == true

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    CenterHeaderBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        navigationIcon = {
            TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_back)) }) {
                SubtleButton(
                    iconOnly = true,
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                ) {
                    Icon(if (isRtl) Icons.Default.ArrowRight else Icons.Default.ArrowLeft, "Back")
                }
            }
        },
        title = {
            // Use type-safe hasRoute() to check current destination
            when {
                currentDestination?.hasRoute(Destination.SecondaryNavigation.Settings::class) == true ->
                    Text(
                        stringResource(Res.string.settings),
                        style = FluentTheme.typography.subtitle,
                        modifier = Modifier.padding(top = 12.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                currentDestination?.hasRoute(Destination.SecondaryNavigation.About::class) == true ->
                    Text(
                        stringResource(Res.string.about),
                        style = FluentTheme.typography.subtitle,
                        modifier = Modifier.padding(top = 12.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
            }
        },
        actions = {
            if (!isPreviousHome) {
                TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_home)) }) {
                    SubtleButton(
                        iconOnly = true,
                        onClick = {
                            navController.navigate(Destination.MainNavigation.Home) {
                                popUpTo(Destination.InitScreen) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp, end = 4.dp)
                    ) { Icon(Icons.Default.Home, "Home") }
                }
            }
        }
    )
}
