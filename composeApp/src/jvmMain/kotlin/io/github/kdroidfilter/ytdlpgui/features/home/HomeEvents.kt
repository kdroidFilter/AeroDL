package io.github.kdroidfilter.ytdlpgui.features.home

sealed class HomeEvents {
    data object OnNextClicked : HomeEvents()
    data class OnLinkChanged(val link: String) : HomeEvents()

    data object OnClipBoardClicked : HomeEvents()
}