package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.status_checking
import ytdlpgui.composeapp.generated.resources.status_downloading
import ytdlpgui.composeapp.generated.resources.status_error
import ytdlpgui.composeapp.generated.resources.status_installed
import ytdlpgui.composeapp.generated.resources.status_pending
import ytdlpgui.composeapp.generated.resources.status_updating

@Composable
internal fun DependencyInfoBar(initState: InitState) {
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        val ytDlpStatus = when {
            initState.checkingYtDlp -> stringResource(Res.string.status_checking)
            initState.downloadingYtDlp -> "${stringResource(Res.string.status_downloading)} ${initState.downloadYtDlpProgress?.let { "(${it.toInt()}%)" } ?: ""}"
            initState.updatingYtdlp -> stringResource(Res.string.status_updating)
            initState.errorMessage != null -> stringResource(Res.string.status_error, initState.errorMessage)
            initState.initCompleted -> stringResource(Res.string.status_installed)
            else -> stringResource(Res.string.status_pending)
        }

        val ffmpegStatus = when {
            initState.checkingFFmpeg -> stringResource(Res.string.status_checking)
            initState.downloadingFFmpeg -> "${stringResource(Res.string.status_downloading)} ${initState.downloadFfmpegProgress?.let { "(${it.toInt()}%)" } ?: ""}"
            initState.updatingFFmpeg -> stringResource(Res.string.status_updating)
            initState.errorMessage != null -> stringResource(Res.string.status_error, initState.errorMessage)
            initState.initCompleted -> stringResource(Res.string.status_installed)
            else -> stringResource(Res.string.status_pending)
        }

        val combinedMessage = "yt-dlp: $ytDlpStatus\nFFmpeg: $ffmpegStatus"

        InfoBar(
            title = { Text("Dependencies") },
            message = { Text(combinedMessage) },
            modifier = Modifier.fillMaxWidth(),
            colors = InfoBarDefaults.colors(),
            icon = { InfoBarDefaults.Badge() },
            closeAction = if (initState.initCompleted) {
                { InfoBarDefaults.CloseActionButton(onClick = { isDismissed = true }) }
            } else {
                null
            }
        )
    }
}

@Preview
@Composable
private fun DependencyInfoBarPendingPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            DependencyInfoBar(
                initState = InitState(
                    initCompleted = false
                )
            )
        }
    }
}

@Preview
@Composable
private fun DependencyInfoBarCheckingPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            DependencyInfoBar(
                initState = InitState(
                    checkingYtDlp = true,
                    checkingFFmpeg = false,
                    initCompleted = false
                )
            )
        }
    }
}

@Preview
@Composable
private fun DependencyInfoBarDownloadingPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            DependencyInfoBar(
                initState = InitState(
                    downloadingYtDlp = true,
                    downloadYtDlpProgress = 65.0F,
                    downloadingFFmpeg = true,
                    downloadFfmpegProgress = 32.0F,
                    initCompleted = false
                )
            )
        }
    }
}

@Preview
@Composable
private fun DependencyInfoBarUpdatingPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            DependencyInfoBar(
                initState = InitState(
                    updatingYtdlp = true,
                    updatingFFmpeg = false,
                    initCompleted = false
                )
            )
        }
    }
}

@Preview
@Composable
private fun DependencyInfoBarCompletedPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            DependencyInfoBar(
                initState = InitState(
                    initCompleted = true
                )
            )
        }
    }
}

@Preview
@Composable
private fun DependencyInfoBarErrorPreview() {
    FluentTheme {
        Column(Modifier.padding(16.dp)) {
            DependencyInfoBar(
                initState = InitState(
                    errorMessage = "Network connection failed",
                    initCompleted = false
                )
            )
        }
    }
}