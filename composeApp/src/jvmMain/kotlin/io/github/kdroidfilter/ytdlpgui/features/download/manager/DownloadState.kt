package io.github.kdroidfilter.ytdlpgui.features.download.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository

data class DownloadState(
    val isLoading: Boolean = false,
    val items: List<DownloadManager.DownloadItem> = emptyList(),
    val history: List<DownloadHistoryRepository.HistoryItem> = emptyList(),
    val directoryAvailability: Map<String, Boolean> = emptyMap(),
)

@Composable
fun collectDownloadState(viewModel: DownloadViewModel): DownloadState =
    DownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
        items = viewModel.items.collectAsState().value,
        history = viewModel.history.collectAsState().value,
        directoryAvailability = viewModel.directoryAvailability.collectAsState(initial = emptyMap()).value,
    )
