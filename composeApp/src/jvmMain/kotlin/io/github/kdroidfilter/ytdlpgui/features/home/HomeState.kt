package io.github.kdroidfilter.ytdlpgui.features.home

data class HomeState(
    val link: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    companion object {
        val loadingState = HomeState(isLoading = true)
        val emptyState = HomeState()
        val errorState = HomeState(errorMessage = "Something went wrong")
    }
}
