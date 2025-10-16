package io.github.kdroidfilter.ytdlpgui.features.onboarding.clipboard

data class ClipboardState(
    val clipboardMonitoringEnabled: Boolean = false
) {
    companion object {
        val enabledState = ClipboardState(clipboardMonitoringEnabled = true)
        val disabledState = ClipboardState(clipboardMonitoringEnabled = false)
    }
}
