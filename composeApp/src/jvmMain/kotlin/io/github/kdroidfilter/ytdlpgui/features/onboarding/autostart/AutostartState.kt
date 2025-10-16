package io.github.kdroidfilter.ytdlpgui.features.onboarding.autostart

data class AutostartState(
    val autoLaunchEnabled: Boolean = false,
) {
    companion object {
        val enabledState = AutostartState(autoLaunchEnabled = true)
        val disabledState = AutostartState(autoLaunchEnabled = false)
    }
}
