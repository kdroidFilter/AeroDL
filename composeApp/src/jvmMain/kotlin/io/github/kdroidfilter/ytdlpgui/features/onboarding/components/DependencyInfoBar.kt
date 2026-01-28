@file:OptIn(ExperimentalFoundationApi::class)

package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.Text
import io.github.composefluent.component.TooltipBox
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Info
import io.github.kdroidfilter.ytdlpgui.core.design.components.ProgressBar
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.dependency_deno_tooltip
import ytdlpgui.composeapp.generated.resources.dependency_ffmpeg_tooltip
import ytdlpgui.composeapp.generated.resources.dependency_info_title
import ytdlpgui.composeapp.generated.resources.dependency_ytdlp_tooltip
import ytdlpgui.composeapp.generated.resources.status_checking
import ytdlpgui.composeapp.generated.resources.status_downloading
import ytdlpgui.composeapp.generated.resources.status_error
import ytdlpgui.composeapp.generated.resources.status_installed
import ytdlpgui.composeapp.generated.resources.status_pending
import ytdlpgui.composeapp.generated.resources.status_updating

@OptIn(ExperimentalFluentApi::class)
@Composable
internal fun DependencyInfoBar(
    initState: InitState,
    isDismissed: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    if (!isDismissed) {
        val ytDlpStatus = when {
            initState.checkingYtDlp -> stringResource(Res.string.status_checking)
            initState.downloadingYtDlp -> stringResource(Res.string.status_downloading)
            initState.updatingYtdlp -> stringResource(Res.string.status_updating)
            initState.errorMessage != null -> stringResource(Res.string.status_error, initState.errorMessage)
            initState.initCompleted -> stringResource(Res.string.status_installed)
            else -> stringResource(Res.string.status_pending)
        }

        val ffmpegStatus = when {
            initState.checkingFFmpeg -> stringResource(Res.string.status_checking)
            initState.downloadingFFmpeg -> stringResource(Res.string.status_downloading)
            initState.updatingFFmpeg -> stringResource(Res.string.status_updating)
            initState.errorMessage != null -> stringResource(Res.string.status_error, initState.errorMessage)
            initState.initCompleted -> stringResource(Res.string.status_installed)
            else -> stringResource(Res.string.status_pending)
        }

        val denoStatus = when {
            initState.checkingDeno -> stringResource(Res.string.status_checking)
            initState.downloadingDeno -> stringResource(Res.string.status_downloading)
            initState.errorMessage != null -> stringResource(Res.string.status_error, initState.errorMessage)
            initState.initCompleted -> stringResource(Res.string.status_installed)
            else -> stringResource(Res.string.status_pending)
        }

        val ytDlpProgress = when {
            initState.downloadYtDlpProgress != null -> initState.downloadYtDlpProgress.div(100f)
            initState.initCompleted -> 1f
            else -> null
        }
        val ffmpegProgress = when {
            initState.downloadFfmpegProgress != null -> initState.downloadFfmpegProgress.div(100f)
            initState.initCompleted -> 1f
            else -> null
        }

        val denoProgress = when {
            initState.downloadDenoProgress != null -> initState.downloadDenoProgress.div(100f)
            initState.initCompleted -> 1f
            else -> null
        }

        InfoBar(
            title = {
                DependencyInfoTitle(
                    text = stringResource(Res.string.dependency_info_title),
                )
            },
            message = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DependencyInfoRow(
                        tooltipText = stringResource(Res.string.dependency_ytdlp_tooltip),
                        label = "Yt-dlp",
                        status = ytDlpStatus,
                        progress = ytDlpProgress
                    )
                    DependencyInfoRow(
                        tooltipText = stringResource(Res.string.dependency_ffmpeg_tooltip),
                        label = "FFmpeg",
                        status = ffmpegStatus,
                        progress = ffmpegProgress
                    )
                    DependencyInfoRow(
                        tooltipText = stringResource(Res.string.dependency_deno_tooltip),
                        label = "Deno",
                        status = denoStatus,
                        progress = denoProgress
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = InfoBarDefaults.colors(),
            icon = { InfoBarDefaults.Badge() },
            closeAction = { InfoBarDefaults.CloseActionButton(onClick = onDismiss) },
        )
    }
}

@OptIn(ExperimentalFluentApi::class)
@Composable
private fun DependencyInfoTitle(
    text: String
) {
    var isOverflow by remember { mutableStateOf(false) }

    val titleText: @Composable () -> Unit = {
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                if (isOverflow != layoutResult.hasVisualOverflow) {
                    isOverflow = layoutResult.hasVisualOverflow
                }
            },
            style = FluentTheme.typography.caption
        )
    }

    if (isOverflow) {
        TooltipBox(
            tooltip = {
                Surface(
                    modifier = Modifier.padding(4.dp),
                    color = Color(0xFF323232),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(8.dp),
                        color = Color.White
                    )
                }
            }
        ) {
            titleText()
        }
    } else {
        titleText()
    }
}

@OptIn(ExperimentalFluentApi::class)
@Composable
private fun DependencyInfoTooltip(
    tooltipText: String
) {
    TooltipBox(
        tooltip = {
            Surface(
                modifier = Modifier.padding(4.dp),
                color = Color(0xFF323232),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = tooltipText,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }
        }
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
    }
}

@Composable
private fun DependencyInfoRow(
    tooltipText: String,
    label: String,
    status: String,
    progress: Float?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DependencyInfoTooltip(
            tooltipText = tooltipText
        )
        Text("$label: $status", style = FluentTheme.typography.caption, fontSize = 8.sp)
        if (progress != null) {
            ProgressBar(
                progress = progress,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }
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