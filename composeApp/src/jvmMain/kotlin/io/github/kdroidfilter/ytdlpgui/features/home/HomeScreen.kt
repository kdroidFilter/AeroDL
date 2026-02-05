package io.github.kdroidfilter.ytdlpgui.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.composefluent.FluentTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppAccentButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppColors
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTextField
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: HomeViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
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
            AppIcon(
                imageVector = AeroDlLogoOnly,
                contentDescription = stringResource(Res.string.logo_content_desc),
                modifier = Modifier.height(150.dp),
                tint = AppColors.neutral
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            when (LocalAppTheme.current) {
                AppTheme.FLUENT -> {
                    io.github.composefluent.component.TextField(
                        value = state.link,
                        enabled = !state.isLoading,
                        onValueChange = { onEvent(HomeEvents.OnLinkChanged(it)) },
                        placeholder = { io.github.composefluent.component.Text(stringResource(Res.string.placeholder_link_example), maxLines = 1) },
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
                            io.github.composefluent.component.Text(
                                text = headerText,
                                style = FluentTheme.typography.caption,
                                textAlign = TextAlign.Center,
                                color = headerColor,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                        }
                    )
                    io.github.composefluent.component.Button(
                        modifier = Modifier.size(33.dp),
                        onClick = { onEvent(HomeEvents.OnClipBoardClicked) },
                        iconOnly = true,
                        disabled = state.isLoading,
                    ) {
                        AppIcon(
                            AppIcons.ClipboardPaste,
                            contentDescription = stringResource(Res.string.paste_link_content_desc)
                        )
                    }
                }
                AppTheme.DARWIN -> {
                    AppTextField(
                        value = state.link,
                        enabled = !state.isLoading,
                        onValueChange = { onEvent(HomeEvents.OnLinkChanged(it)) },
                        placeholder = stringResource(Res.string.placeholder_link_example),
                        singleLine = true,
                        label = when {
                            state.isLoading -> stringResource(Res.string.loading)
                            state.errorMessage != null -> when (state.errorMessage) {
                                HomeError.SingleValidUrl -> stringResource(Res.string.error_single_valid_url)
                                HomeError.InvalidUrlFormat -> stringResource(Res.string.error_invalid_url_format)
                                HomeError.UrlRequired -> stringResource(Res.string.error_url_required)
                            }
                            else -> stringResource(Res.string.paste_video_link_header)
                        }
                    )
                    AppButton(
                        modifier = Modifier.size(33.dp),
                        onClick = { onEvent(HomeEvents.OnClipBoardClicked) },
                        enabled = !state.isLoading,
                    ) {
                        AppIcon(
                            AppIcons.ClipboardPaste,
                            contentDescription = stringResource(Res.string.paste_link_content_desc)
                        )
                    }
                }
            }
        }
        AppAccentButton(
            onClick = { onEvent(HomeEvents.OnNextClicked) },
            enabled = !state.isLoading
        ) {
            AppText(stringResource(Res.string.next))
            AppIcon(if (isRtl) AppIcons.ArrowLeft else AppIcons.ArrowRight, contentDescription = null)
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
