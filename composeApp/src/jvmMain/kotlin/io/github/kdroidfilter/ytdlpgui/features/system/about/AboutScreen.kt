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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppHyperlinkButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun AboutScreen() {
    val viewModel: AboutViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()
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
        ?: stringResource(Res.string.app_version_label, "\u2014")

    val ytdlpVersion = state.ytdlpVersion?.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.ytdlp_version_label, it) }
        ?: stringResource(Res.string.ytdlp_version_missing)

    val ffmpegVersion = state.ffmpegVersion?.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.ffmpeg_version_label, it) }
        ?: stringResource(Res.string.ffmpeg_version_missing)

    val denoVersion = state.denoVersion?.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.deno_version_label, it) }
        ?: stringResource(Res.string.deno_version_missing)

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
                AppText(
                    text = stringResource(Res.string.about_credits_title),
                    style = AppTypography.subtitle,
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = stringResource(Res.string.credits_made_by),
                    style = AppTypography.body,
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = stringResource(Res.string.credits_thank_jetbrains),
                    style = AppTypography.body,
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = stringResource(Res.string.credits_thank_oss),
                    style = AppTypography.body,
                    textAlign = TextAlign.Center
                )
                AppHyperlinkButton(
                    onClick = { openUrlInBrowser("https://github.com/kdroidFilter") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    AppText(stringResource(Res.string.credits_github_prompt))
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
                AppText(
                    text = stringResource(Res.string.about_versions_title),
                    style = AppTypography.bodyStrong,
                    textAlign = TextAlign.Center
                )
                AppText(text = appVersion, style = AppTypography.body, textAlign = TextAlign.Center)
                AppText(text = ytdlpVersion, style = AppTypography.body, textAlign = TextAlign.Center)
                AppText(text = ffmpegVersion, style = AppTypography.body, textAlign = TextAlign.Center)
                AppText(text = denoVersion, style = AppTypography.body, textAlign = TextAlign.Center)
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
