package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.VideoInfo

data class SingleDownloadState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val videoInfo: VideoInfo? = null,
    val availablePresets: List<YtDlpWrapper.Preset> = emptyList(),
    val selectedPreset: YtDlpWrapper.Preset? = null,
    val availableSubtitleLanguages: List<String> = emptyList(),
    val selectedSubtitles: List<String> = emptyList(),
)

@Composable
fun collectSingleDownloadState(viewModel: SingleDownloadViewModel): SingleDownloadState =
    SingleDownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
        errorMessage = viewModel.errorMessage.collectAsState().value,
        videoInfo = viewModel.videoInfo.collectAsState().value,
        availablePresets = viewModel.availablePresets.collectAsState().value,
        selectedPreset = viewModel.selectedPreset.collectAsState().value,
        availableSubtitleLanguages = viewModel.availableSubtitleLanguages.collectAsState().value,
        selectedSubtitles = viewModel.selectedSubtitles.collectAsState().value,
    )
