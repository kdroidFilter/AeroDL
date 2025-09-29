package io.github.kdroidfilter.ytdlpgui.features.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class HistoryState(
    val isLoading: Boolean = false,
)

@Composable
fun collectHistoryState(viewModel: HistoryViewModel): HistoryState =
    HistoryState(
        isLoading = viewModel.isLoading.collectAsState().value,
    )
