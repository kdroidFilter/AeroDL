package io.github.kdroidfilter.ytdlpgui.features.screens.download.bulkdownload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class BulkDownloadState(
    val isLoading: Boolean = false,
)

@Composable
fun collectBulkDownloadState(viewModel: BulkDownloadViewModel): BulkDownloadState =
    BulkDownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
    )
