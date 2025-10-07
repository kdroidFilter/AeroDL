package io.github.kdroidfilter.ytdlpgui.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Extension function for NavGraphBuilder that adds a composable with standard animations.
 * This function helps avoid repetition of animation code across different composables.
 *
 * @param T The destination type that extends Destination
 * @param content The composable content to display
 */
inline fun <reified T : Destination> NavGraphBuilder.noAnimatedComposable(
    noinline content: @Composable (NavBackStackEntry) -> Unit
) {
    composable<T>(
        enterTransition = { NavigationAnimations.enterTransition(this) },
        exitTransition = { NavigationAnimations.exitTransition(this) },
        popEnterTransition = { NavigationAnimations.popEnterTransition(this) },
        popExitTransition = { NavigationAnimations.popExitTransition(this) }
    ) {
        content(it)
    }
}