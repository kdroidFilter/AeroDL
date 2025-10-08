package io.github.kdroidfilter.ytdlpgui.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Button
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_progress_label
import ytdlpgui.composeapp.generated.resources.next
import ytdlpgui.composeapp.generated.resources.onboarding_previous
import ytdlpgui.composeapp.generated.resources.onboarding_skip
import ytdlpgui.composeapp.generated.resources.status_checking
import ytdlpgui.composeapp.generated.resources.status_downloading
import ytdlpgui.composeapp.generated.resources.status_updating
import ytdlpgui.composeapp.generated.resources.status_installed
import ytdlpgui.composeapp.generated.resources.status_pending
import ytdlpgui.composeapp.generated.resources.status_error

@Composable
internal fun HeaderRow(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(title)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle)
        }
    }
}

@Composable
internal fun OnboardingProgress(
    step: OnboardingStep,
    modifier: Modifier = Modifier,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
) {
    val steps = remember { OnboardingStep.entries }
    val actualCurrentIndex = currentStepIndex ?: (steps.indexOf(step).takeIf { it >= 0 } ?: 0)
    val actualTotalSteps = totalSteps ?: steps.size
    val progress = ((actualCurrentIndex + 1).toFloat() / actualTotalSteps.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = stringResource(Res.string.onboarding_progress_label, actualCurrentIndex + 1, actualTotalSteps),
            style = FluentTheme.typography.caption
        )
        Spacer(Modifier.height(6.dp))
        ProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
        if (initState != null) {
            Spacer(Modifier.height(12.dp))
            DependencyInfoBar(initState)
        }
    }
}

@Composable
private fun DependencyInfoBar(initState: InitState) {
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

@Composable
internal fun NavigationRow(
    onNext: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    nextLabel: String? = null,
    previousLabel: String? = null,
    skipLabel: String? = null,
    nextEnabled: Boolean = true
) {
    val resolvedNext = nextLabel ?: stringResource(Res.string.next)
    val resolvedPrevious = previousLabel ?: stringResource(Res.string.onboarding_previous)
    val resolvedSkip = skipLabel ?: stringResource(Res.string.onboarding_skip)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            if (onPrevious != null) {
                Button(
                    onClick = onPrevious,
                    content = { Text(resolvedPrevious) }
                )
            }
        }
        Row {
            if (onSkip != null) {
                Button(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = onSkip,
                    content = { Text(resolvedSkip) }
                )
            }
            Button(
                onClick = onNext,
                disabled = !nextEnabled,
                content = { Text(resolvedNext) }
            )
        }
    }
}
