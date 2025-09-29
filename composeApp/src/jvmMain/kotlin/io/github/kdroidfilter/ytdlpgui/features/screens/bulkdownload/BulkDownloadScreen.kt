package io.github.kdroidfilter.ytdlpgui.features.screens.bulkdownload

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BulkDownloadScreen() {
    val viewModel = koinViewModel<BulkDownloadViewModel>()
    val state = collectBulkDownloadState(viewModel)
    BulkDownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun BulkDownloadView(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit,
) {
    Text("Bulk Download Screen")
}