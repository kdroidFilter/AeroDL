package io.github.kdroidfilter.ytdlpgui.features.screens.singledownload

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SingleDownloadScreen() {
    val viewModel = koinViewModel<SingleDownloadViewModel>()
    val state = collectSingleDownloadState(viewModel)
    SingleDownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun SingleDownloadView(
    state: SingleDownloadState,
    onEvent: (SingleDownloadEvents) -> Unit,
) {
    Text("Single Download Screen")
}