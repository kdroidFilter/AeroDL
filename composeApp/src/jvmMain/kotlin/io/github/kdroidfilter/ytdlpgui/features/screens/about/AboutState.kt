package io.github.kdroidfilter.ytdlpgui.features.screens.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class AboutState(
    val isLoading: Boolean = false,
)

@Composable
fun collectAboutState(viewModel: AboutViewModel): AboutState =
    AboutState(
        isLoading = viewModel.isLoading.collectAsState().value,
    )
