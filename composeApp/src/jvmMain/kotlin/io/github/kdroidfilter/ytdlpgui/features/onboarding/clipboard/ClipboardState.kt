package io.github.kdroidfilter.ytdlpgui.features.onboarding.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class ClipboardState(
    val clipboardMonitoringEnabled: Boolean = false
) {
    companion object {
        val enabledState = ClipboardState(clipboardMonitoringEnabled = true)
        val disabledState = ClipboardState(clipboardMonitoringEnabled = false)
    }
}

@Composable
fun collectClipboardState(viewModel: OnboardingViewModel): ClipboardState = ClipboardState(
    clipboardMonitoringEnabled = viewModel.clipboardMonitoringEnabled.collectAsState().value
)
