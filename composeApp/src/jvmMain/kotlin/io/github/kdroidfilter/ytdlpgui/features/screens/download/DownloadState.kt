package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class DownloadState(
    val isLoading: Boolean = false,
)

@Composable
fun collectDownloadState(viewModel: DownloadViewModel): DownloadState =
    DownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
    )
