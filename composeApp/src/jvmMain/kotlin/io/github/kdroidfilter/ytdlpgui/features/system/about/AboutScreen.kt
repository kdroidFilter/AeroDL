package io.github.kdroidfilter.ytdlpgui.features.system.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Text
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutScreen() {
    val viewModel = koinViewModel<AboutViewModel>()
    val state = collectAboutState(viewModel)
    AboutView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun collectAboutState(viewModel: AboutViewModel): AboutState {
    return viewModel.state.collectAsState().value
}

@Composable
fun AboutView(
    state: AboutState,
    onEvent: (AboutEvents) -> Unit,
) {
    Column {
        Text(stringResource(Res.string.about_screen_title))
        Spacer(Modifier.height(16.dp))
        Text("Version: ${state.appVersion}")
        Text("yt-dlp version: ${state.ytdlpVersion ?: "Not found"}")
    }
}