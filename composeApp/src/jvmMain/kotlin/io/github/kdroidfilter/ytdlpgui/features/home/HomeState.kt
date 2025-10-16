package io.github.kdroidfilter.ytdlpgui.features.home

import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination

enum class HomeError {
    SingleValidUrl,
    InvalidUrlFormat,
    UrlRequired,
}

sealed class HomeNavigationState {
    data object None : HomeNavigationState()
    data class NavigateToDownload(val destination: Destination.Download) : HomeNavigationState()
}

data class HomeState(
    val link: String = "",
    val isLoading: Boolean = false,
    val errorMessage: HomeError? = null,
    val navigationState: HomeNavigationState = HomeNavigationState.None
) {
    companion object {
        val loadingState = HomeState(isLoading = true)
        val emptyState = HomeState()
        val errorState = HomeState(errorMessage = HomeError.InvalidUrlFormat)
    }
}
