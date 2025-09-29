package io.github.kdroidfilter.ytdlpgui.features.screens.home

sealed class HomeEvents {
    data object Download : HomeEvents()
}