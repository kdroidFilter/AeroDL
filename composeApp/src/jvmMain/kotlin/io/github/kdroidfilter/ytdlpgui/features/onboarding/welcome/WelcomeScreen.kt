package io.github.kdroidfilter.ytdlpgui.features.onboarding.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingNavigationState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.DependencyInfoBar
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_start
import ytdlpgui.composeapp.generated.resources.onboarding_welcome_subtitle
import ytdlpgui.composeapp.generated.resources.onboarding_welcome_title

@Composable
fun WelcomeScreen(navController: NavHostController) {
    val appGraph = LocalAppGraph.current
    val viewModel = remember(appGraph) { appGraph.onboardingViewModel }
    val state by viewModel.uiState.collectAsState()
    
    // Handle navigation following the same pattern as SingleDownloadScreen
    LaunchedEffect(state.navigationState) {
        when (val navState = state.navigationState) {
            is OnboardingNavigationState.NavigateToStep -> {
                println("WelcomeScreen: Navigating to ${navState.destination}")
                navController.navigate(navState.destination)
            }
            is OnboardingNavigationState.NavigateToHome -> {
                navController.navigate(Destination.MainNavigation.Home)
            }
            OnboardingNavigationState.None -> {
                // no-op
            }
        }
    }
    
    WelcomeView(
        onEvent = viewModel::handleEvent,
        currentStep = state.currentStep,
        initState = viewModel.initState.collectAsState().value,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        dependencyInfoBarDismissed = state.dependencyInfoBarDismissed
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
