package io.github.kdroidfilter.ytdlpgui.features.onboarding

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val dependencyInfoBarDismissed: Boolean = false,
)