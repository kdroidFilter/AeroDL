package io.github.kdroidfilter.ytdlpgui.features.onboarding.finish

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.core.design.components.ProgressBar
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.emitter.Emitter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.*
import kotlin.time.Duration.Companion.seconds

@Composable
fun FinishScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    LaunchedEffect(Unit) { viewModel.onEvents(OnboardingEvents.OnFinish) }
    FinishView(
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        onComplete = { viewModel.completeOnboarding() }
    )
}

@Composable
private fun FinishView(
    currentStep: OnboardingStep = OnboardingStep.Finish,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    onComplete: () -> Unit = {}
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
            LoadingResourcesScreen(initState = initState)
        }
    }
}

@Composable
private fun LoadingResourcesScreen(
    initState: InitState? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.onboarding_finish_loading_title),
            style = FluentTheme.typography.subtitle
        )

        Text(
            text = stringResource(Res.string.onboarding_finish_loading_message),
            style = FluentTheme.typography.bodyStrong
        )

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
