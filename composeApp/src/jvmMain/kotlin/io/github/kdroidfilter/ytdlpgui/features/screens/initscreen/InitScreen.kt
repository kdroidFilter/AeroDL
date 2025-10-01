package io.github.kdroidfilter.ytdlpgui.features.screens.initscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.checking_ffmpeg
import ytdlpgui.composeapp.generated.resources.checking_ytdlp
import ytdlpgui.composeapp.generated.resources.downloading_ffmpeg
import ytdlpgui.composeapp.generated.resources.downloading_ytdlp
import ytdlpgui.composeapp.generated.resources.error_occurred
import ytdlpgui.composeapp.generated.resources.updating_ffmpeg
import ytdlpgui.composeapp.generated.resources.updating_ytdlp
import kotlin.math.roundToInt

@Composable
fun InitScreen() {
    val viewModel = koinViewModel<InitViewModel>()
    val state = collectInitState(viewModel)
    InitView(state)
}

@Composable
fun InitView(state: InitState){
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (state.errorMessage == null) {
            ProgressRing(Modifier.size(33.dp))
            Spacer(Modifier.size(16.dp))
        }

        when {
            state.checkingYtDlp -> Text(text = stringResource(Res.string.checking_ytdlp))
            state.checkingFFmpeg -> Text(text = stringResource(Res.string.checking_ffmpeg))
            state.downloadingYtDlp -> Text(text = stringResource(Res.string.downloading_ytdlp))
            state.downloadingFFmpeg -> Text(text = stringResource(Res.string.downloading_ffmpeg))
            state.updatingYtdlp -> Text(text = stringResource(Res.string.updating_ytdlp))
            state.updatingFFmpeg -> Text(text = stringResource(Res.string.updating_ffmpeg))
            state.errorMessage != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Row {
                        Text(text = stringResource(Res.string.error_occurred))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.errorMessage,
                            color = FluentTheme.colors.system.critical
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(8.dp))

        if (state.downloadingYtDlp && state.downloadYtDlpProgress != null) {
            Text(text = "${(state.downloadYtDlpProgress).roundToInt()}%")
        } else {
            Spacer(Modifier.height(FluentTheme.typography.caption.fontSize.value.dp))
        }
        if (state.downloadingFFmpeg && state.downloadFfmpegProgress != null) {
            Text(text = "${(state.downloadFfmpegProgress).roundToInt()}%")
        } else {
            Spacer(Modifier.height(FluentTheme.typography.caption.fontSize.value.dp))
        }
    }
}