package io.github.kdroidfilter.ytdlpgui.features.onboarding

import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination

sealed class OnboardingNavigationState {
    data object None : OnboardingNavigationState()
    data class NavigateToStep(val destination: Destination.Onboarding) : OnboardingNavigationState()
    data object NavigateToHome : OnboardingNavigationState()
}

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val dependencyInfoBarDismissed: Boolean = false,
    val navigationState: OnboardingNavigationState = OnboardingNavigationState.None
)