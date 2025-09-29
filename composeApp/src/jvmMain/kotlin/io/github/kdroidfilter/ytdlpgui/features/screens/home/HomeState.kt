package io.github.kdroidfilter.ytdlpgui.features.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

data class HomeState(
    val link: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
fun collectHomeState(viewModel: HomeViewModel) : HomeState =
    HomeState(
        link = viewModel.textFieldContent.collectAsState().value,
        isLoading = viewModel.isLoading.collectAsState().value,
        errorMessage = viewModel.errorMessage.collectAsState().value,
    )