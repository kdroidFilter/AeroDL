package io.github.kdroidfilter.ytdlpgui.core.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object MainGraph: Destination

    @Serializable
    data object HomeScreen: Destination

    @Serializable
    data class SingleDownloadScreen(val url: String): Destination

    @Serializable
    data class BulkDownloadScreen(val url: String): Destination

    @Serializable
    data object SettingsScreen: Destination

    @Serializable
    data object HistoryScreen: Destination

    @Serializable
    data object AboutScreen: Destination

}