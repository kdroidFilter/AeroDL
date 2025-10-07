package io.github.kdroidfilter.ytdlpgui.features.onboarding.includepreset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class IncludePresetState(
    val includePresetInFilename: Boolean = true
) {
    companion object {
        val enabledState = IncludePresetState(includePresetInFilename = true)
        val disabledState = IncludePresetState(includePresetInFilename = false)
    }
}

@Composable
fun collectIncludePresetState(viewModel: OnboardingViewModel): IncludePresetState = IncludePresetState(
    includePresetInFilename = viewModel.includePresetInFilename.collectAsState().value
)
