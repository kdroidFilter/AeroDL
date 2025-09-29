package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class SingleDownloadState(
    val isLoading: Boolean = false,
)

@Composable
fun collectSingleDownloadState(viewModel: SingleDownloadViewModel): SingleDownloadState =
    SingleDownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
    )
