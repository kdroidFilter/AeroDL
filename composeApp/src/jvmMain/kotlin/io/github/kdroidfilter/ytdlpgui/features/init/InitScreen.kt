package io.github.kdroidfilter.ytdlpgui.features.init

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
import io.github.kdroidfilter.ytdlpgui.core.design.icons.AeroDlLogoOnly
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
        Icon(AeroDlLogoOnly, null, modifier = Modifier.height(100.dp))
        Spacer(Modifier.size(32.dp))

        if (state.errorMessage == null) {
            if (state.checkingYtDlp) Text(text = stringResource(Res.string.checking_ytdlp))
            if (state.downloadingYtDlp) {
                val progress = (state.downloadYtDlpProgress ?: 0f)
                Text(text = stringResource(Res.string.downloading_ytdlp) + " ${progress.toInt()}%")
            }
            if (state.updatingYtdlp) Text(text = stringResource(Res.string.updating_ytdlp))

            Spacer(Modifier.height(8.dp))

            if (state.checkingFFmpeg) Text(text = stringResource(Res.string.checking_ffmpeg))
            if (state.downloadingFFmpeg) {
                val progress = (state.downloadFfmpegProgress ?: 0f)
                Text(text = stringResource(Res.string.downloading_ffmpeg) + " ${progress.toInt()}%")
            }
            if (state.updatingFFmpeg) Text(text = stringResource(Res.string.updating_ffmpeg))
        } else {
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