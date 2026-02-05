package io.github.kdroidfilter.ytdlpgui.features.onboarding.autostart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSwitcher
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import androidx.navigation.NavHostController
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.DependencyInfoBar
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.HeaderRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.NavigationRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.common_disabled
import ytdlpgui.composeapp.generated.resources.common_enabled
import ytdlpgui.composeapp.generated.resources.settings_auto_launch_caption
import ytdlpgui.composeapp.generated.resources.settings_auto_launch_title

@Composable
fun AutostartScreen(navController: NavHostController) {
    val viewModel: OnboardingViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val onboardingUiState by viewModel.uiState.collectAsState()
    val autoLaunchEnabled by viewModel.autoLaunchEnabled.collectAsState()
    val state = AutostartState(
        autoLaunchEnabled = autoLaunchEnabled
    )
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    val dependencyInfoBarDismissed by viewModel.dependencyInfoBarDismissed.collectAsState()

    // Navigation driven by ViewModel state
    LaunchedEffect(onboardingUiState.navigationState) {
        when (val navState = onboardingUiState.navigationState) {
            is io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingNavigationState.NavigateToStep -> {
                navController.navigate(navState.destination)
                viewModel.handleEvent(OnboardingEvents.OnNavigationConsumed)
            }
            is io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingNavigationState.NavigateToHome -> {
                navController.navigate(io.github.kdroidfilter.ytdlpgui.core.navigation.Destination.MainNavigation.Home)
                viewModel.handleEvent(OnboardingEvents.OnNavigationConsumed)
            }
            io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingNavigationState.None -> Unit
        }
    }

    AutostartView(
        state = state,
        onEvent = viewModel::handleEvent,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        dependencyInfoBarDismissed = dependencyInfoBarDismissed,
    )
}

@Composable
fun AutostartView(
    state: AutostartState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Autostart,
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
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HeaderRow(
                title = stringResource(Res.string.settings_auto_launch_title),
                subtitle = stringResource(Res.string.settings_auto_launch_caption)
            )
            Spacer(Modifier.height(12.dp))
            AppIcon(AppIcons.Power, null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppSwitcher(
                    checked = state.autoLaunchEnabled,
                    onCheckedChange = { onEvent(OnboardingEvents.OnSetAutoLaunchEnabled(it)) }
                )
                Spacer(Modifier.width(8.dp))
                val statusLabel = if (state.autoLaunchEnabled) {
                    stringResource(Res.string.common_enabled)
                } else {
                    stringResource(Res.string.common_disabled)
                }
                AppText(statusLabel)
            }
        }
        if (initState != null) {
            DependencyInfoBar(
                initState = initState,
                isDismissed = dependencyInfoBarDismissed,
                onDismiss = { onEvent(OnboardingEvents.OnDismissDependencyInfoBar) }
            )
        }
        NavigationRow(
            onNext = { onEvent(OnboardingEvents.OnNext) },
            onPrevious = { onEvent(OnboardingEvents.OnPrevious) }
        )
    }
}

@Preview
@Composable
fun AutostartScreenPreviewEnabled() {
    AutostartView(
        state = AutostartState.enabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun AutostartScreenPreviewDisabled() {
    AutostartView(
        state = AutostartState.disabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
