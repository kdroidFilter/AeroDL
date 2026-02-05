package io.github.kdroidfilter.ytdlpgui.features.onboarding.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kdroidfilter.ytdlpgui.core.design.components.ProgressBar
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppInfoBar
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppInfoBarSeverity
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTooltip
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
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

        val title = stringResource(Res.string.dependency_info_title)
        val ytDlpLabel = "Yt-dlp: $ytDlpStatus"
        val ffmpegLabel = "FFmpeg: $ffmpegStatus"
        val denoLabel = "Deno: $denoStatus"
        val message = "$ytDlpLabel | $ffmpegLabel | $denoLabel"

        AppInfoBar(
            title = title,
            message = message,
            severity = AppInfoBarSeverity.Info,
            modifier = Modifier.fillMaxWidth(),
            onDismiss = onDismiss,
        )

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
        AppTooltip(tooltip = tooltipText) {
            AppIcon(
                imageVector = AppIcons.Info,
                contentDescription = "Info",
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
        }
        AppText("$label: $status", style = AppTypography.caption, fontSize = 8.sp)
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
    Column(Modifier.padding(16.dp)) {
        DependencyInfoBar(
            initState = InitState(
                initCompleted = false
            )
        )
    }
}

@Preview
@Composable
private fun DependencyInfoBarCheckingPreview() {
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

@Preview
@Composable
private fun DependencyInfoBarDownloadingPreview() {
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

@Preview
@Composable
private fun DependencyInfoBarUpdatingPreview() {
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

@Preview
@Composable
private fun DependencyInfoBarCompletedPreview() {
    Column(Modifier.padding(16.dp)) {
        DependencyInfoBar(
            initState = InitState(
                initCompleted = true
            )
        )
    }
}

@Preview
@Composable
private fun DependencyInfoBarErrorPreview() {
    Column(Modifier.padding(16.dp)) {
        DependencyInfoBar(
            initState = InitState(
                errorMessage = "Network connection failed",
                initCompleted = false
            )
        )
    }
}
