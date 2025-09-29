package io.github.kdroidfilter.ytdlpgui.features.screens.about

import androidx.compose.runtime.Composable
import io.github.composefluent.component.Text
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
fun AboutView(
    state: AboutState,
    onEvent: (AboutEvents) -> Unit,
) {
    Text("About Screen")
}