package io.github.kdroidfilter.ytdlpgui.features.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class SettingsState(
    val isLoading: Boolean = false,
)

@Composable
fun collectSettingsState(viewModel: SettingsViewModel): SettingsState =
    SettingsState(
        isLoading = viewModel.isLoading.collectAsState().value,
    )
