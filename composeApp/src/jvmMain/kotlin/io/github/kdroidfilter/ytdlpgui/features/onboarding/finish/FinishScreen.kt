package io.github.kdroidfilter.ytdlpgui.features.onboarding.finish

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.core.design.components.ProgressBar
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import androidx.navigation.NavHostController
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingNavigationState
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import io.github.composefluent.component.ContentDialog
import io.github.composefluent.component.DialogSize
import io.github.composefluent.component.SubtleButton
import io.github.kdroidfilter.ytdlpgui.core.design.components.TerminalView
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.emitter.Emitter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.*
import io.github.kdroidfilter.ytdlpgui.features.init.InitEvent
import kotlin.time.Duration.Companion.seconds

@Composable
fun FinishScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = LocalAppGraph.current.onboardingViewModel,
) {
    val appGraph = LocalAppGraph.current
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    val state by viewModel.uiState.collectAsState()

    // Navigation driven by ViewModel state
    androidx.compose.runtime.LaunchedEffect(state.navigationState) {
        when (val navState = state.navigationState) {
            is OnboardingNavigationState.NavigateToStep -> {
                navController.navigate(navState.destination)
                viewModel.handleEvent(OnboardingEvents.OnNavigationConsumed)
            }
            is OnboardingNavigationState.NavigateToHome -> {
                navController.navigate(Destination.MainNavigation.Home)
                viewModel.handleEvent(OnboardingEvents.OnNavigationConsumed)
            }
            OnboardingNavigationState.None -> Unit
        }
    }
    FinishView(
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        onComplete = { viewModel.completeOnboarding() },
        onRetryInit = { appGraph.initViewModel.handleEvent(InitEvent.StartInitialization) }
    )
}

@Composable
private fun FinishView(
    currentStep: OnboardingStep = OnboardingStep.Finish,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    onComplete: () -> Unit = {},
    onRetryInit: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
    ) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )

        Spacer(Modifier.height(32.dp))

        if (initState?.initCompleted == true) {
            ReadyToGoScreen(onComplete = onComplete)
        } else {
            LoadingResourcesScreen(
                initState = initState,
                onRetryInit = onRetryInit,
            )
        }
    }
}

@Composable
private fun LoadingResourcesScreen(
    initState: InitState? = null,
    onRetryInit: () -> Unit = {},
) {
    var showErrorDetails by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (initState?.errorMessage != null) {
            Text(
                text = stringResource(Res.string.error_occurred),
                style = FluentTheme.typography.subtitle,
                color = FluentTheme.colors.system.critical,
            )
            Text(
                text = initState.errorMessage,
                style = FluentTheme.typography.bodyStrong,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.onboarding_finish_error_hint),
                style = FluentTheme.typography.body,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(Res.string.onboarding_finish_loading_title),
                style = FluentTheme.typography.subtitle
            )

            Text(
                text = stringResource(Res.string.onboarding_finish_loading_message),
                style = FluentTheme.typography.bodyStrong
            )
        }

        Spacer(Modifier.height(16.dp))

        // Show progress bars for yt-dlp
        if (initState?.checkingYtDlp == true || initState?.downloadingYtDlp == true || initState?.updatingYtdlp == true) {
            Text("yt-dlp")
            if (initState.downloadYtDlpProgress != null) {
                ProgressBar(
                    progress = initState.downloadYtDlpProgress.div(100f),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ProgressBar(modifier = Modifier.fillMaxWidth())
            }
        }

        // Show progress bars for FFmpeg
        if (initState?.checkingFFmpeg == true || initState?.downloadingFFmpeg == true || initState?.updatingFFmpeg == true) {
            Text("FFmpeg")
            if (initState.downloadFfmpegProgress != null) {
                ProgressBar(
                    progress = initState.downloadFfmpegProgress.div(100f),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ProgressBar(modifier = Modifier.fillMaxWidth())
            }
        }

        // Action when error occurs
        if (initState?.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentButton(onClick = onRetryInit) { Text(stringResource(Res.string.onboarding_finish_retry)) }
                SubtleButton(onClick = { showErrorDetails = true }) {
                    Text(stringResource(Res.string.view_error_details))
                }
            }
        }
    }

    if (showErrorDetails && (initState?.errorMessage != null)) {
        ContentDialog(
            title = stringResource(Res.string.download_error_title),
            visible = true,
            size = DialogSize.Min,
            primaryButtonText = stringResource(Res.string.ok),
            onButtonClick = { showErrorDetails = false },
            content = {
                TerminalView(text = initState.errorMessage)
            }
        )
    }
}

@Composable
private fun ReadyToGoScreen(
    onComplete: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        ConfettiKit(
            modifier = Modifier.fillMaxSize(),
            parties = listOf(
                Party(emitter = Emitter(duration = 5.seconds).perSecond(30))
            )
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = stringResource(Res.string.onboarding_finish_ready_title),
                style = FluentTheme.typography.subtitle
            )

            Text(
                text = stringResource(Res.string.onboarding_finish_ready_message),
                style = FluentTheme.typography.bodyStrong,
                textAlign = TextAlign.Center
            )

            Image(painterResource(Res.drawable.Rocket), null, Modifier.size(148.dp).padding(vertical = 16.dp))

            AccentButton(onClick = onComplete) {
                Text(stringResource(Res.string.onboarding_finish_ready_button))
            }
        }
    }

}

@Preview
@Composable
fun FinishScreenPreview() {
    FinishView(
        initState = InitState(initCompleted = true)
    )
}

@Preview
@Composable
fun LoadingResourcesScreenPreview() {
    LoadingResourcesScreen(
        initState = InitState(
            downloadingYtDlp = true,
            downloadYtDlpProgress = 45.5f,
            downloadingFFmpeg = true,
            downloadFfmpegProgress = 72.3f
        )
    )
}

@Preview
@Composable
fun LoadingResourcesScreenCheckingPreview() {
    LoadingResourcesScreen(
        initState = InitState(
            checkingYtDlp = true,
            checkingFFmpeg = true
        )
    )
}

@Preview
@Composable
fun ReadyToGoScreenPreview() {
    ReadyToGoScreen()
}
