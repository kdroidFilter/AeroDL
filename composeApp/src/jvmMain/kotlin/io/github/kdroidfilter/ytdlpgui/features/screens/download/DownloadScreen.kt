package io.github.kdroidfilter.ytdlpgui.features.screens.download

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadScreen() {
    val viewModel = koinViewModel<DownloadViewModel>()
    val state = collectDownloadState(viewModel)
    DownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun DownloadView(
    state: DownloadState,
    onEvent: (DownloadEvents) -> Unit,
) {
    Text(stringResource(Res.string.history_screen_title))
}