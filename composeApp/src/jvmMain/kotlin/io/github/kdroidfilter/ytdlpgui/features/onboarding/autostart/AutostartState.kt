package io.github.kdroidfilter.ytdlpgui.features.onboarding.autostart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class AutostartState(
    val autoLaunchEnabled: Boolean = false,
) {
    companion object {
        val enabledState = AutostartState(autoLaunchEnabled = true)
        val disabledState = AutostartState(autoLaunchEnabled = false)
    }
}

@Composable
fun collectAutostartState(viewModel: OnboardingViewModel): AutostartState = AutostartState(
    autoLaunchEnabled = viewModel.autoLaunchEnabled.collectAsState().value
)
