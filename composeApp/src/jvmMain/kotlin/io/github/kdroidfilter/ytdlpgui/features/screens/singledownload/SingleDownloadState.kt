package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlp.model.VideoInfo

data class SingleDownloadState(
    val isLoading: Boolean = false,
    val videoInfo: VideoInfo? = null,
)

@Composable
fun collectSingleDownloadState(viewModel: SingleDownloadViewModel): SingleDownloadState =
    SingleDownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
        videoInfo = viewModel.videoInfo.collectAsState().value,
    )
