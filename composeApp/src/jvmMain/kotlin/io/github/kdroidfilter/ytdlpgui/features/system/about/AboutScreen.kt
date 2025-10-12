package io.github.kdroidfilter.ytdlpgui.features.system.about

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.HyperlinkButton
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun AboutScreen() {
    val viewModel = koinViewModel<AboutViewModel>()
    val state = viewModel.uiState.collectAsState().value
    AboutView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
fun AboutView(
    state: AboutState,
    onEvent: (AboutEvents) -> Unit,
) {
    val appVersion = state.appVersion.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.app_version_label, it) }
        ?: stringResource(Res.string.app_version_label, "—")

    val ytdlpVersion = state.ytdlpVersion?.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.ytdlp_version_label, it) }
        ?: stringResource(Res.string.ytdlp_version_missing)

    val ffmpegVersion = state.ffmpegVersion?.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.ffmpeg_version_label, it) }
        ?: stringResource(Res.string.ffmpeg_version_missing)

    val scrollState = rememberScrollState()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.about_credits_title),
                    style = FluentTheme.typography.subtitle,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.credits_made_by),
                    style = FluentTheme.typography.body,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.credits_thank_jetbrains),
                    style = FluentTheme.typography.body,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.credits_thank_oss),
                    style = FluentTheme.typography.body,
                    textAlign = TextAlign.Center
                )
                HyperlinkButton(
                    onClick = { openUrlInBrowser("https://github.com/kdroidFilter") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(Res.string.credits_github_prompt))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Divider()
                Text(
                    text = stringResource(Res.string.about_versions_title),
                    style = FluentTheme.typography.bodyStrong,
                    textAlign = TextAlign.Center
                )
                Text(text = appVersion, style = FluentTheme.typography.body, textAlign = TextAlign.Center)
                Text(text = ytdlpVersion, style = FluentTheme.typography.body, textAlign = TextAlign.Center)
                Text(text = ffmpegVersion, style = FluentTheme.typography.body, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 2.dp, start = 8.dp)
        )
    }
}
