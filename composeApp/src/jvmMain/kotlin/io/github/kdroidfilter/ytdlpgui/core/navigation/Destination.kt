package io.github.kdroidfilter.ytdlpgui.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data object InitScreen: Destination

    @Serializable
    sealed interface MainNavigation : Destination {
        @Serializable
        data object Graph : MainNavigation

        @Serializable
        data object Home : MainNavigation

        @Serializable
        data object Downloader : MainNavigation
    }

    @Serializable
    sealed interface SecondaryNavigation : Destination {
        @Serializable
        data object Graph : SecondaryNavigation

        @Serializable
        data object Settings : SecondaryNavigation

        @Serializable
        data object About : SecondaryNavigation
    }

    // Onboarding flow
    @Serializable
    sealed interface Onboarding : Destination {
        @Serializable
        data object Graph : Onboarding

        @Serializable
        data object Welcome : Onboarding

        @Serializable
        data object DownloadDir : Onboarding

        @Serializable
        data object Cookies : Onboarding

        @Serializable
        data object NoCheckCert : Onboarding

        @Serializable
        data object GnomeFocus : Onboarding

        @Serializable
        data object Clipboard : Onboarding

        @Serializable
        data object Autostart : Onboarding

        @Serializable
        data object Finish : Onboarding
    }

    // Nested Download graph containing Single and Bulk download flows
    @Serializable
    sealed interface Download : Destination {
        // Graph container to use with navigation<Destination.Download.Graph>
        @Serializable
        data object Graph : Download
        @Serializable
        data class Single(val videoLink: String) : Download
        @Serializable
        data class Bulk(val url: String) : Download
    }

    // Converter screen for local file conversion
    @Serializable
    sealed interface Converter : Destination {
        @Serializable
        data object Graph : Converter

        @Serializable
        data object Input : Converter

        @Serializable
        data class Options(val filePath: String) : Converter
    }

}
