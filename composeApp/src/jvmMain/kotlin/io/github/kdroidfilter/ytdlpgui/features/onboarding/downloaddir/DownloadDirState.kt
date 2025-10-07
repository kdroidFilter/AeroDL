package io.github.kdroidfilter.ytdlpgui.features.onboarding.downloaddir

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class DownloadDirState(
    val downloadDirPath: String = ""
) {
    companion object {
        val emptyState = DownloadDirState()
        val configuredState = DownloadDirState(
            downloadDirPath = "/home/user/Downloads"
        )
    }
}

@Composable
fun collectDownloadDirState(viewModel: OnboardingViewModel): DownloadDirState =
    DownloadDirState(
        downloadDirPath = viewModel.downloadDirPath.collectAsState().value
    )