package io.github.kdroidfilter.ytdlpgui.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

// Represents the global app initialization state
// Defaults allow simple AppState() construction

data class AppState(
    val checkingYtDlp: Boolean = false,
    val checkingFFmpeg: Boolean = false,

    val downloadingYtDlp: Boolean = false,
    val downloadYtDlpProgress: Float? = null,

    val downloadingFFmpeg: Boolean = false,
    val downloadFfmpegProgress: Float? = null,

    val errorMessage: String? = null,

    val updatingYtdlp: Boolean = false,
    val updatingFFmpeg: Boolean = false,

    val initCompleted: Boolean = false,
)

@Composable
fun collectAppState(viewModel: AppViewModel) : AppState {
    return viewModel.state.collectAsState().value
}