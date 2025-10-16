package io.github.kdroidfilter.ytdlpgui.features.onboarding.nocheckcert

data class NoCheckCertState(
    val noCheckCertificate: Boolean = false
) {
    companion object {
        val enabledState = NoCheckCertState(noCheckCertificate = true)
        val disabledState = NoCheckCertState(noCheckCertificate = false)
    }
}
