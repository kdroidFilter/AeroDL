package io.github.kdroidfilter.ytdlpgui.features.onboarding.parallel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class ParallelState(
    val parallelDownloads: Int = 2
) {
    companion object {
        val defaultState = ParallelState()
    }
}

@Composable
fun collectParallelState(viewModel: OnboardingViewModel): ParallelState = ParallelState(
    parallelDownloads = viewModel.parallelDownloads.collectAsState().value
)
