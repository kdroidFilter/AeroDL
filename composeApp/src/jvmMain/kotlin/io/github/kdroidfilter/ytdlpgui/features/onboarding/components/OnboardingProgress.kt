package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_progress_label

@Composable
internal fun OnboardingProgress(
    step: OnboardingStep,
    modifier: Modifier = Modifier,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
) {
    val steps = remember { OnboardingStep.entries }
    val actualCurrentIndex = currentStepIndex ?: (steps.indexOf(step).takeIf { it >= 0 } ?: 0)
    val actualTotalSteps = totalSteps ?: steps.size
    val progress = ((actualCurrentIndex + 1).toFloat() / actualTotalSteps.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = stringResource(Res.string.onboarding_progress_label, actualCurrentIndex + 1, actualTotalSteps),
            style = FluentTheme.typography.caption
        )
        Spacer(Modifier.height(6.dp))
        ProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
        if (initState != null) {
            Spacer(Modifier.height(12.dp))
            DependencyInfoBar(initState)
        }
    }
}

@Preview
@Composable
private fun OnboardingProgressFirstStepPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            OnboardingProgress(
                step = OnboardingStep.Welcome,
                currentStepIndex = 0,
                totalSteps = 7
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingProgressMiddleStepPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            OnboardingProgress(
                step = OnboardingStep.Cookies,
                currentStepIndex = 3,
                totalSteps = 7
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingProgressWithInitStateDownloadingPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            OnboardingProgress(
                step = OnboardingStep.Welcome,
                initState = InitState(
                    downloadingYtDlp = true,
                    downloadYtDlpProgress = 45.0F,
                    checkingFFmpeg = true,
                    initCompleted = false
                ),
                currentStepIndex = 0,
                totalSteps = 7
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingProgressWithInitStateCompletedPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            OnboardingProgress(
                step = OnboardingStep.Welcome,
                initState = InitState(initCompleted = true),
                currentStepIndex = 0,
                totalSteps = 7
            )
        }
    }
}