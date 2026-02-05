package io.github.kdroidfilter.ytdlpgui.features.onboarding.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppAccentButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppColors
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingNavigationState
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
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
    val viewModel: OnboardingViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()

    // Navigation driven by ViewModel state (like HomeScreen)
    LaunchedEffect(state.navigationState) {
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
            AppText(
                text = stringResource(Res.string.onboarding_welcome_title),
                style = AppTypography.subtitle
            )
            AppIcon(AeroDlLogoOnly, null, modifier = Modifier.height(96.dp), tint = AppColors.neutral)
            io.github.kdroidfilter.ytdlpgui.features.onboarding.components.ExpandableDescription(
                description = stringResource(Res.string.onboarding_welcome_subtitle),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            AppAccentButton(onClick = { onEvent(OnboardingEvents.OnStart) }) {
                AppText(stringResource(Res.string.onboarding_start))
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
