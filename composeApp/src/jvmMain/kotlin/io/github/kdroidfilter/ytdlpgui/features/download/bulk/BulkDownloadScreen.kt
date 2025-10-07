package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
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
    Text(stringResource(Res.string.bulk_download_screen_title))
}