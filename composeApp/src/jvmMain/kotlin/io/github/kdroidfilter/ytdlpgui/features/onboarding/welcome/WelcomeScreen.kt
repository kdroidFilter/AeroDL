package io.github.kdroidfilter.ytdlpgui.features.onboarding.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Button
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.DependencyInfoBar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_start
import ytdlpgui.composeapp.generated.resources.onboarding_welcome_subtitle
import ytdlpgui.composeapp.generated.resources.onboarding_welcome_title

@Composable
fun WelcomeScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    WelcomeView(
        onEvent = viewModel::onEvents,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex()
    )
}

@Composable
fun WelcomeView(
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Welcome,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(Res.string.onboarding_welcome_title))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.onboarding_welcome_subtitle))
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onEvent(OnboardingEvents.OnStart) }) {
                Text(stringResource(Res.string.onboarding_start))
            }
        }
        if (initState != null) {
            DependencyInfoBar(initState)
        }
    }
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    WelcomeView(
        onEvent = {},
        initState = InitState(checkingYtDlp = true)
    )
}
