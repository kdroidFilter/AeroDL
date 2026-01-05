package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.HyperlinkButton
import io.github.composefluent.component.SubtleButton
import io.github.kdroidfilter.ytdlpgui.core.design.components.AnimatedGears
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner

@Composable
fun BulkDownloadScreen() {
    val viewModel: BulkDownloadViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()
    BulkDownloadView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
fun BulkDownloadView(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        AnimatedGears(modifier = Modifier.fillMaxWidth(0.75f).height(128.dp).padding(top = 16.dp))
        Text(
            text = stringResource(Res.string.bulk_download_support_notice),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.75f),
        )
        HyperlinkButton(
            onClick = { openUrlInBrowser("https://ko-fi.com/lomityaesh") },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(stringResource(Res.string.support_on_kofi))
        }
    }
}
