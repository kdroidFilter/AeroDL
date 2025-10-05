package io.github.kdroidfilter.ytdlpgui.features.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.ClipboardPaste
import io.github.composefluent.icons.regular.ArrowLeft
import io.github.composefluent.icons.regular.ArrowRight
import io.github.kdroidfilter.ytdlpgui.core.presentation.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.presentation.tools.PreviewContainer
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun HomeScreen() {
    val viewModel = koinInject<HomeViewModel>()
    val state = collectHomeState(viewModel)
    HomeView(
        state = state,
        onEvent = viewModel::onEvents,
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
                        state.errorMessage != null -> state.errorMessage to FluentTheme.colors.system.critical
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
    PreviewContainer {
        HomeView(state = HomeState.emptyState, onEvent = {})
    }
}

@Preview
@Composable
fun HomeScreenPreviewLoading() {
    PreviewContainer {
        HomeView(state = HomeState.loadingState, onEvent = {})
    }
}