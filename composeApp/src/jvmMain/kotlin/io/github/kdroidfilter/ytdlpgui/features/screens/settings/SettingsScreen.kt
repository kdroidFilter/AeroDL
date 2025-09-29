package io.github.kdroidfilter.ytdlpgui.features.screens.settings

import io.github.composefluent.component.Text
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state = collectSettingsState(viewModel)
    SettingsView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun SettingsView(
    state: SettingsState,
    onEvent: (SettingsEvents) -> Unit,
) {
    Text("Settings Screen")
}