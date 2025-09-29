package io.github.kdroidfilter.ytdlpgui.features.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class HomeState(
    val link: String = "",
    val isLoading: Boolean = false,
)

@Composable
fun collectHomeState(viewModel: HomeViewModel) : HomeState =
    HomeState(
        link = viewModel.link.collectAsState().value,
        isLoading = viewModel.isLoading.collectAsState().value,
    )