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
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Power
import io.github.kdroidfilter.ytdlpgui.core.design.components.Switcher
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
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
    val appGraph = LocalAppGraph.current
    val viewModel = remember(appGraph) { appGraph.onboardingViewModel }
    val autoLaunchEnabled by viewModel.autoLaunchEnabled.collectAsState()
    val state = AutostartState(
        autoLaunchEnabled = autoLaunchEnabled
    )
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    val dependencyInfoBarDismissed by viewModel.dependencyInfoBarDismissed.collectAsState()
    

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
            Icon(Icons.Regular.Power, null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switcher(
                    checked = state.autoLaunchEnabled,
                    onCheckStateChange = { onEvent(OnboardingEvents.OnSetAutoLaunchEnabled(it)) }
                )
                Spacer(Modifier.width(8.dp))
                val statusLabel = if (state.autoLaunchEnabled) {
                    stringResource(Res.string.common_enabled)
                } else {
                    stringResource(Res.string.common_disabled)
                }
                Text(statusLabel)
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
