package io.github.kdroidfilter.ytdlpgui.features.onboarding.cookies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Icon
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.Cookies
import io.github.kdroidfilter.ytdlpgui.core.design.components.BrowserSelector
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
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_label
import ytdlpgui.composeapp.generated.resources.settings_cookies_from_browser_title
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun CookiesScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val cookiesFromBrowser by viewModel.cookiesFromBrowser.collectAsState()
    val state = CookiesState(
        cookiesFromBrowser = cookiesFromBrowser
    )
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    val dependencyInfoBarDismissed by viewModel.dependencyInfoBarDismissed.collectAsState()
    CookiesView(
        state = state,
        onEvent = viewModel::handleEvent,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        dependencyInfoBarDismissed = dependencyInfoBarDismissed
    )
}

@Composable
fun CookiesView(
    state: CookiesState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Cookies,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    dependencyInfoBarDismissed: Boolean = false,
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
    ) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HeaderRow(
                title = stringResource(Res.string.settings_cookies_from_browser_title),
                subtitle = stringResource(Res.string.settings_cookies_from_browser_label)
            )
            Icon(Icons.Filled.Cookies, null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            BrowserSelector(
                currentBrowser = state.cookiesFromBrowser,
                onBrowserSelected = { browser ->
                    onEvent(OnboardingEvents.OnSetCookiesFromBrowser(browser))
                }
            )
        }
        if (initState != null) {
            DependencyInfoBar(
                initState = initState,
                isDismissed = dependencyInfoBarDismissed,
                onDismiss = { onEvent(OnboardingEvents.OnDismissDependencyInfoBar) }
            )
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
