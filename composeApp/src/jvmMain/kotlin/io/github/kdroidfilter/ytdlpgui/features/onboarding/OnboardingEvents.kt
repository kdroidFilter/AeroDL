package io.github.kdroidfilter.ytdlpgui.features.onboarding

sealed class OnboardingEvents {
    data object OnStart : OnboardingEvents()
    data object OnNext : OnboardingEvents()
    data object OnSkip : OnboardingEvents()
    data object OnFinish : OnboardingEvents()

    data class OnPickDownloadDir(val title: String) : OnboardingEvents()
    data class OnSetCookiesFromBrowser(val browser: String) : OnboardingEvents()
    data class OnSetIncludePresetInFilename(val include: Boolean) : OnboardingEvents()
    data class OnSetParallelDownloads(val count: Int) : OnboardingEvents()
    data class OnSetNoCheckCertificate(val enabled: Boolean) : OnboardingEvents()
    data class OnSetClipboardMonitoring(val enabled: Boolean) : OnboardingEvents()
}