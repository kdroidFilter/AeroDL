package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.core.business.DownloadManager

data class DownloadState(
    val isLoading: Boolean = false,
    val items: List<DownloadManager.DownloadItem> = emptyList(),
    val history: List<io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository.HistoryItem> = emptyList(),
)

@Composable
fun collectDownloadState(viewModel: DownloadViewModel): DownloadState =
    DownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
        items = viewModel.items.collectAsState().value,
        history = viewModel.history.collectAsState().value,
    )
