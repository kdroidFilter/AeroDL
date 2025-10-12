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
import androidx.compose.ui.unit.sp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
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
    val dependencyInfoBarDismissed by viewModel.dependencyInfoBarDismissed.collectAsState()
    WelcomeView(
        onEvent = viewModel::handleEvent,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        dependencyInfoBarDismissed = dependencyInfoBarDismissed
    )
}

@Composable
fun WelcomeView(
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Welcome,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    dependencyInfoBarDismissed: Boolean = false,
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
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.onboarding_welcome_title),
                style = FluentTheme.typography.subtitle
            )
            Icon(AeroDlLogoOnly, null, modifier = Modifier.height(96.dp), tint = FluentTheme.colors.system.neutral)
            io.github.kdroidfilter.ytdlpgui.features.onboarding.components.ExpandableDescription(
                description = stringResource(Res.string.onboarding_welcome_subtitle),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            AccentButton(onClick = { onEvent(OnboardingEvents.OnStart) }) {
                Text(stringResource(Res.string.onboarding_start))
            }
        }
        if (initState != null) {
            DependencyInfoBar(
                initState = initState,
                isDismissed = dependencyInfoBarDismissed,
                onDismiss = { onEvent(OnboardingEvents.OnDismissDependencyInfoBar) }
            )
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
