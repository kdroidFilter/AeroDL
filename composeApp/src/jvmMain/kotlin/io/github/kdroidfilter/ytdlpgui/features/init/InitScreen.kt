package io.github.kdroidfilter.ytdlpgui.features.init

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.checking_ffmpeg
import ytdlpgui.composeapp.generated.resources.checking_ytdlp
import ytdlpgui.composeapp.generated.resources.downloading_ffmpeg
import ytdlpgui.composeapp.generated.resources.downloading_ytdlp
import ytdlpgui.composeapp.generated.resources.error_occurred
 
import ytdlpgui.composeapp.generated.resources.updating_ffmpeg
import ytdlpgui.composeapp.generated.resources.updating_ytdlp
import ytdlpgui.composeapp.generated.resources.checking_deno
import ytdlpgui.composeapp.generated.resources.downloading_deno
import ytdlpgui.composeapp.generated.resources.init_retry

@Composable
fun InitScreen(navController: NavHostController) {
    val viewModel: InitViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()
    
    // Handle navigation based on state
    LaunchedEffect(state.navigationState) {
        when (val navigationState = state.navigationState) {
            is InitNavigationState.NavigateToOnboarding -> {
                navController.navigate(Destination.Onboarding.Graph) {
                    popUpTo(Destination.InitScreen) { inclusive = true }
                    launchSingleTop = true
                }
                viewModel.handleEvent(InitEvent.NavigationConsumed)
            }
            is InitNavigationState.NavigateToHome -> {
                navController.navigate(Destination.MainNavigation.Home) {
                    popUpTo(Destination.InitScreen) { inclusive = true }
                    launchSingleTop = true
                }
                viewModel.handleEvent(InitEvent.NavigationConsumed)
            }
            InitNavigationState.None -> {
                // No navigation needed
            }
        }
    }
    
    InitView(
        state = state,
        onRetry = { viewModel.handleEvent(InitEvent.StartInitialization) },
    )
}

@Composable
fun InitView(
    state: InitState,
    onRetry: () -> Unit = {},
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(AeroDlLogoOnly, null, modifier = Modifier.height(100.dp))
        Spacer(Modifier.size(32.dp))

        if (state.errorMessage == null) {
            // Show progress ring when any operation is in progress
            val isInProgress = state.checkingYtDlp || state.downloadingYtDlp || state.updatingYtdlp ||
                    state.checkingFFmpeg || state.downloadingFFmpeg || state.updatingFFmpeg ||
                    state.checkingDeno || state.downloadingDeno

            if (isInProgress) {
                ProgressRing(modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
            }

            if (state.checkingYtDlp) Text(text = stringResource(Res.string.checking_ytdlp))
            if (state.downloadingYtDlp) {
                val progress = (state.downloadYtDlpProgress ?: 0f)
                Text(text = stringResource(Res.string.downloading_ytdlp) + " ${progress.toInt()}%")
            }
            if (state.updatingYtdlp) Text(text = stringResource(Res.string.updating_ytdlp))

            if (state.checkingFFmpeg) Text(text = stringResource(Res.string.checking_ffmpeg))
            if (state.downloadingFFmpeg) {
                val progress = (state.downloadFfmpegProgress ?: 0f)
                Text(text = stringResource(Res.string.downloading_ffmpeg) + " ${progress.toInt()}%")
            }
            if (state.updatingFFmpeg) Text(text = stringResource(Res.string.updating_ffmpeg))

            if (state.checkingDeno) Text(text = stringResource(Res.string.checking_deno))
            if (state.downloadingDeno) {
                val progress = (state.downloadDenoProgress ?: 0f)
                Text(text = stringResource(Res.string.downloading_deno) + " ${progress.toInt()}%")
            }
        } else {
            Row {
                Text(text = stringResource(Res.string.error_occurred))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.errorMessage, color = FluentTheme.colors.system.critical
                )
            }
            Spacer(Modifier.height(16.dp))
            AccentButton(onClick = onRetry) {
                Text(stringResource(Res.string.init_retry))
            }
        }
    }
}

@Preview
@Composable
fun InitScreenPreviewCheckingYtDlp() {
    InitView(state = InitState.checkingYtDlpState)
}

@Preview
@Composable
fun InitScreenPreviewDownloadingYtDlp() {
    InitView(state = InitState.downloadingYtDlpState)
}

@Preview
@Composable
fun InitScreenPreviewUpdatingYtDlp() {
    InitView(state = InitState.updatingYtDlpState)
}

@Preview
@Composable
fun InitScreenPreviewCheckingFFmpeg() {
    InitView(state = InitState.checkingFFmpegState)
}

@Preview
@Composable
fun InitScreenPreviewDownloadingFFmpeg() {
    InitView(state = InitState.downloadingFFmpegState)
}

@Preview
@Composable
fun InitScreenPreviewUpdatingFFmpeg() {
    InitView(state = InitState.updatingFFmpegState)
}

@Preview
@Composable
fun InitScreenPreviewError() {
    InitView(state = InitState.errorState)
}

@Preview
@Composable
fun InitScreenPreviewCompleted() {
    InitView(state = InitState.completedState)
}

@Preview
@Composable
fun InitScreenPreviewDownloadingBoth() {
    InitView(state = InitState.downloadingBothState)
}
