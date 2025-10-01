package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import io.github.kdroidfilter.ytdlp.model.VideoInfo
import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object MainGraph: Destination

    @Serializable
    data object InitScreen: Destination

    @Serializable
    data object HomeScreen: Destination

    @Serializable
    data class SingleDownloadScreen(val videoLink: String): Destination

    @Serializable
    data class BulkDownloadScreen(val url: String): Destination

    @Serializable
    data object SettingsScreen: Destination

    @Serializable
    data object HistoryScreen: Destination

    @Serializable
    data object AboutScreen: Destination

}