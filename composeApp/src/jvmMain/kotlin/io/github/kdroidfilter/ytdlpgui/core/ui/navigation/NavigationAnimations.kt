package io.github.kdroidfilter.ytdlpgui.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry

/**
 * JVM implementation of navigation animations
 * Returns null for all transitions to keep the default behavior
 */
 object NavigationAnimations {
     fun enterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition? {
        return null // Default animation for JVM
    }

     fun exitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition? {
        return null // Default animation for JVM
    }

     fun popEnterTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition? {
        return null // Default animation for JVM
    }

    fun popExitTransition(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition? {
        return null // Default animation for JVM
    }
}