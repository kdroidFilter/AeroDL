package io.github.kdroidfilter.ytdlpgui.features.home

enum class HomeError {
    SingleValidUrl,
    InvalidUrlFormat,
    UrlRequired,
}

data class HomeState(
    val link: String = "",
    val isLoading: Boolean = false,
    val errorMessage: HomeError? = null,
) {
    companion object {
        val loadingState = HomeState(isLoading = true)
        val emptyState = HomeState()
        val errorState = HomeState(errorMessage = HomeError.InvalidUrlFormat)
    }
}
