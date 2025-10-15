package io.github.kdroidfilter.ytdlpgui.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.ClipboardPaste
import io.github.composefluent.icons.regular.ArrowLeft
import io.github.composefluent.icons.regular.ArrowRight
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun HomeScreen(navController: NavHostController) {
    val appGraph = LocalAppGraph.current
        val viewModel = remember(appGraph) { appGraph.homeViewModel }
    val state by viewModel.uiState.collectAsState()
    
    // Handle navigation
    LaunchedEffect(state.navigationState) {
        when (val navigationState = state.navigationState) {
            is HomeNavigationState.NavigateToDownload -> {
                navController.navigate(navigationState.destination)
                viewModel.handleEvent(HomeEvents.OnNavigationConsumed)
            }
            HomeNavigationState.None -> {
                // No navigation needed
            }
        }
    }
    
    HomeView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
fun HomeView(
    state: HomeState,
    onEvent: (HomeEvents) -> Unit,
) {
    val currentLayoutDirection = LocalLayoutDirection.current
    val isRtl = (currentLayoutDirection == LayoutDirection.Rtl)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = AeroDlLogoOnly,
                contentDescription = stringResource(Res.string.logo_content_desc),
                modifier = Modifier.height(150.dp),
                tint = FluentTheme.colors.system.neutral
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = state.link,
                enabled = !state.isLoading,
                onValueChange = { onEvent(HomeEvents.OnLinkChanged(it)) },
                placeholder = { Text(stringResource(Res.string.placeholder_link_example), maxLines = 1) },
                singleLine = true,
                header = {
                    val (headerText, headerColor) = when {
                        state.isLoading -> stringResource(Res.string.loading) to FluentTheme.colors.text.text.tertiary
                        state.errorMessage != null -> {
                            val msg = when (state.errorMessage) {
                                HomeError.SingleValidUrl -> stringResource(Res.string.error_single_valid_url)
                                HomeError.InvalidUrlFormat -> stringResource(Res.string.error_invalid_url_format)
                                HomeError.UrlRequired -> stringResource(Res.string.error_url_required)
                            }
                            msg to FluentTheme.colors.system.critical
                        }
                        else -> stringResource(Res.string.paste_video_link_header) to FluentTheme.colors.text.text.disabled
                    }
                    Text(
                        text = headerText,
                        style = FluentTheme.typography.caption,
                        textAlign = TextAlign.Center,
                        color = headerColor,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            )
            Button(
                modifier = Modifier.size(33.dp),
                onClick = { onEvent(HomeEvents.OnClipBoardClicked) },
                iconOnly = true,
                disabled = state.isLoading,
            ) {
                Icon(
                    Icons.Filled.ClipboardPaste,
                    contentDescription = stringResource(Res.string.paste_link_content_desc)
                )
            }
        }
        AccentButton(
            onClick = { onEvent(HomeEvents.OnNextClicked) },
            disabled = state.isLoading
        ) {
            Text(stringResource(Res.string.next))
            Icon(if (isRtl) Icons.Default.ArrowLeft else Icons.Default.ArrowRight, contentDescription = null)
        }
    }

}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeView(state = HomeState.emptyState, onEvent = {})
}

@Preview
@Composable
fun HomeScreenPreviewLoading() {
    HomeView(state = HomeState.loadingState, onEvent = {})
}
