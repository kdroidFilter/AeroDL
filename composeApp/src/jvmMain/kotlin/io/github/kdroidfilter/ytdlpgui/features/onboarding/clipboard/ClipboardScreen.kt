package io.github.kdroidfilter.ytdlpgui.features.onboarding.clipboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Power
import io.github.kdroidfilter.ytdlpgui.core.design.components.Switcher
import io.github.kdroidfilter.ytdlpgui.features.onboarding.HeaderRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.NavigationRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingProgress
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.common_disabled
import ytdlpgui.composeapp.generated.resources.common_enabled
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_caption
import ytdlpgui.composeapp.generated.resources.settings_clipboard_monitoring_title
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun ClipboardScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state = collectClipboardState(viewModel)
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    ClipboardView(
        state = state,
        onEvent = viewModel::onEvents,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex()
    )
}

@Composable
fun ClipboardView(
    state: ClipboardState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Clipboard,
    initState: io.github.kdroidfilter.ytdlpgui.features.init.InitState? = null,
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
        Column(Modifier.weight(1f).fillMaxWidth()) {
            HeaderRow(
                title = stringResource(Res.string.settings_clipboard_monitoring_title),
                subtitle = stringResource(Res.string.settings_clipboard_monitoring_caption)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switcher(
                    checked = state.clipboardMonitoringEnabled,
                    onCheckStateChange = { onEvent(OnboardingEvents.OnSetClipboardMonitoring(it)) }
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Regular.Power, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                val statusLabel = if (state.clipboardMonitoringEnabled) {
                    stringResource(Res.string.common_enabled)
                } else {
                    stringResource(Res.string.common_disabled)
                }
                Text(statusLabel)
            }
        }
        NavigationRow(
            onNext = { onEvent(OnboardingEvents.OnNext) },
            onPrevious = { onEvent(OnboardingEvents.OnPrevious) },
            onSkip = { onEvent(OnboardingEvents.OnSkip) }
        )
    }
}

@Preview
@Composable
fun ClipboardScreenPreviewEnabled() {
    ClipboardView(
        state = ClipboardState.enabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun ClipboardScreenPreviewDisabled() {
    ClipboardView(
        state = ClipboardState.disabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
