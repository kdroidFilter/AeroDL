package io.github.kdroidfilter.ytdlpgui.features.onboarding.includepreset

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Text
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
import ytdlpgui.composeapp.generated.resources.settings_include_preset_in_filename_caption
import ytdlpgui.composeapp.generated.resources.settings_include_preset_in_filename_title
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun IncludePresetScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state = collectIncludePresetState(viewModel)
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    IncludePresetView(
        state = state,
        onEvent = viewModel::onEvents,
        currentStep = currentStep,
        initState = initState
    )
}

@Composable
fun IncludePresetView(
    state: IncludePresetState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.IncludePreset,
    initState: io.github.kdroidfilter.ytdlpgui.features.init.InitState? = null,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OnboardingProgress(step = currentStep, initState = initState)
        Column(Modifier.weight(1f).fillMaxWidth()) {
            HeaderRow(
                title = stringResource(Res.string.settings_include_preset_in_filename_title),
                subtitle = stringResource(Res.string.settings_include_preset_in_filename_caption)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switcher(
                    checked = state.includePresetInFilename,
                    onCheckStateChange = { onEvent(OnboardingEvents.OnSetIncludePresetInFilename(it)) }
                )
                Spacer(Modifier.width(8.dp))
                val statusLabel = if (state.includePresetInFilename) {
                    stringResource(Res.string.common_enabled)
                } else {
                    stringResource(Res.string.common_disabled)
                }
                Text(statusLabel)
            }
        }
        NavigationRow(
            onNext = { onEvent(OnboardingEvents.OnNext) },
            onSkip = { onEvent(OnboardingEvents.OnSkip) }
        )
    }
}

@Preview
@Composable
fun IncludePresetScreenPreviewEnabled() {
    IncludePresetView(
        state = IncludePresetState.enabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun IncludePresetScreenPreviewDisabled() {
    IncludePresetView(
        state = IncludePresetState.disabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
