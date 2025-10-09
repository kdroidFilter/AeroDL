package io.github.kdroidfilter.ytdlpgui.features.onboarding.finish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Button
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_complete_message
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

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
    initState: io.github.kdroidfilter.ytdlpgui.features.init.InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    onComplete: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )
        Text(stringResource(Res.string.onboarding_complete_message))

        Spacer(Modifier.height(32.dp))

        if (initState?.initCompleted == true) {
            Button(onClick = onComplete) {
                Text("C'est parti !")
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Préparation en cours...")
                Spacer(Modifier.height(16.dp))

                // Show progress bars for yt-dlp
                if (initState?.checkingYtDlp == true || initState?.downloadingYtDlp == true || initState?.updatingYtdlp == true) {
                    Text("yt-dlp")
                    Spacer(Modifier.height(8.dp))
                    if (initState.downloadYtDlpProgress != null) {
                        ProgressBar(progress = initState.downloadYtDlpProgress)
                    } else {
                        ProgressBar()
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Show progress bars for FFmpeg
                if (initState?.checkingFFmpeg == true || initState?.downloadingFFmpeg == true || initState?.updatingFFmpeg == true) {
                    Text("FFmpeg")
                    Spacer(Modifier.height(8.dp))
                    if (initState.downloadFfmpegProgress != null) {
                        ProgressBar(progress = initState.downloadFfmpegProgress)
                    } else {
                        ProgressBar()
                    }
                }
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
