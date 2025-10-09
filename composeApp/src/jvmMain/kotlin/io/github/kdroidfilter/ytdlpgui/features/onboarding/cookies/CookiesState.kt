package io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel

data class CookiesState(
    val cookiesFromBrowser: String = ""
) {
    companion object {
        val emptyState = CookiesState()
        val chromeState = CookiesState(cookiesFromBrowser = "chrome")
        val firefoxState = CookiesState(cookiesFromBrowser = "firefox")
    }
}

@Composable
fun collectCookiesState(viewModel: OnboardingViewModel): CookiesState =
    CookiesState(
        cookiesFromBrowser = viewModel.cookiesFromBrowser.collectAsState().value
    )