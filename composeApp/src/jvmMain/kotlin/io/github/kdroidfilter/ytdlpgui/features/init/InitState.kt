package io.github.kdroidfilter.ytdlpgui.features.init

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class InitState(
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
fun collectInitState(viewModel: InitViewModel) : InitState {
    return viewModel.state.collectAsState().value
}