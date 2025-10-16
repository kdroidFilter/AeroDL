package io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies

data class CookiesState(
    val cookiesFromBrowser: String = ""
) {
    companion object {
        val emptyState = CookiesState()
        val chromeState = CookiesState(cookiesFromBrowser = "chrome")
        val firefoxState = CookiesState(cookiesFromBrowser = "firefox")
    }
}
