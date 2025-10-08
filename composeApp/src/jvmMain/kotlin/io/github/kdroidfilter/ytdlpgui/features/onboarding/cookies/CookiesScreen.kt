package io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Cookies
import io.github.kdroidfilter.ytdlpgui.core.design.icons.BrowserChrome
import io.github.kdroidfilter.ytdlpgui.core.design.icons.BrowserFirefox
import io.github.kdroidfilter.ytdlpgui.core.design.icons.Cookie_off
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.HeaderRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.NavigationRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.OnboardingProgress
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import io.github.kdroidfilter.ytdlpgui.features.onboarding.components.DependencyInfoBar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.settings_browser_chrome
import ytdlpgui.composeapp.generated.resources.settings_browser_disable
import ytdlpgui.composeapp.generated.resources.settings_browser_firefox
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_label
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_title
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun CookiesScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state = collectCookiesState(viewModel)
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    CookiesView(
        state = state,
        onEvent = viewModel::onEvents,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex()
    )
}

@Composable
fun CookiesView(
    state: CookiesState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Cookies,
    initState: io.github.kdroidfilter.ytdlpgui.features.init.InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )
        Column(Modifier.weight(1f).fillMaxWidth()) {
            HeaderRow(
                title = stringResource(Res.string.settings_cookies_from_browser_title),
                subtitle = stringResource(Res.string.settings_cookies_from_browser_label)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chromeLabel = stringResource(Res.string.settings_browser_chrome)
                Button(onClick = { onEvent(OnboardingEvents.OnSetCookiesFromBrowser("chrome")) }) {
                    Icon(BrowserChrome, chromeLabel, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(chromeLabel)
                }
                val firefoxLabel = stringResource(Res.string.settings_browser_firefox)
                Button(onClick = { onEvent(OnboardingEvents.OnSetCookiesFromBrowser("firefox")) }) {
                    Icon(BrowserFirefox, firefoxLabel, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(firefoxLabel)
                }
                val disableLabel = stringResource(Res.string.settings_browser_disable)
                Button(onClick = { onEvent(OnboardingEvents.OnSetCookiesFromBrowser("")) }) {
                    Icon(Cookie_off, disableLabel, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(disableLabel)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Regular.Cookies, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                val browserLabel = when (state.cookiesFromBrowser.lowercase()) {
                    "" -> stringResource(Res.string.settings_browser_disable)
                    "chrome" -> stringResource(Res.string.settings_browser_chrome)
                    "firefox" -> stringResource(Res.string.settings_browser_firefox)
                    else -> state.cookiesFromBrowser
                }
                Text(browserLabel)
            }
        }
        if (initState != null) {
            DependencyInfoBar(initState)
        }
        NavigationRow(
            onNext = { onEvent(OnboardingEvents.OnNext) },
            onPrevious = { onEvent(OnboardingEvents.OnPrevious) }
        )
    }
}

@Preview
@Composable
fun CookiesScreenPreviewEmpty() {
    CookiesView(
        state = CookiesState.emptyState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun CookiesScreenPreviewChrome() {
    CookiesView(
        state = CookiesState.chromeState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun CookiesScreenPreviewFirefox() {
    CookiesView(
        state = CookiesState.firefoxState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
