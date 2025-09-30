package io.github.kdroidfilter.ytdlpgui.features.screens.history

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen() {
    val viewModel = koinViewModel<HistoryViewModel>()
    val state = collectHistoryState(viewModel)
    HistoryView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun HistoryView(
    state: HistoryState,
    onEvent: (HistoryEvents) -> Unit,
) {
    Text(stringResource(Res.string.history_screen_title))
}