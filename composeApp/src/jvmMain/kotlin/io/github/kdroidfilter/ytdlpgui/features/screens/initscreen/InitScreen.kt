package io.github.kdroidfilter.ytdlpgui.features.screens.initscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.core.ui.icons.AeroDlLogoOnly
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
fun InitView(state: InitState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val percent = if (state.downloadingYtDlp && state.downloadYtDlpProgress != null) state.downloadYtDlpProgress
        else if (state.downloadingFFmpeg && state.downloadFfmpegProgress != null) state.downloadFfmpegProgress
        else 0.0

        val percentText = if (percent != 0.0) "${(percent.toFloat()).roundToInt()}%" else ""

        if (percent != 0.0) {
            Icon(AeroDlLogoOnly, null, modifier = Modifier.height(100.dp))

            Spacer(Modifier.size(32.dp))
        }

        if (state.errorMessage == null) {
            Box(
                modifier = Modifier.size(33.dp)
            ) {
                if (percent != 0.0) ProgressRing(
                    progress = percent.toFloat() / 100,
                    Modifier.fillMaxSize()
                ) else ProgressRing(Modifier.fillMaxSize())

                Text(
                    percentText,
                    style = FluentTheme.typography.caption,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.offset(x = if (percent == 100.0f) 2.dp else 4.dp, y = 8.dp)
                )
            }
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
                            text = state.errorMessage, color = FluentTheme.colors.system.critical
                        )
                    }
                }
            }
        }
    }
}