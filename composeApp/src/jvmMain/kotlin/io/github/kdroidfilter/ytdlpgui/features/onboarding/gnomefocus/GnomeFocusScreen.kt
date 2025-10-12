package io.github.kdroidfilter.ytdlpgui.features.onboarding.gnomefocus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.HyperlinkButton
import io.github.composefluent.component.Text
import io.github.kdroidfilter.ytdlpgui.features.init.InitState
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
import ytdlpgui.composeapp.generated.resources.onboarding_gnome_focus_caption
import ytdlpgui.composeapp.generated.resources.onboarding_gnome_focus_open_extension
import ytdlpgui.composeapp.generated.resources.onboarding_gnome_focus_title

@Composable
fun GnomeFocusScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    val dependencyInfoBarDismissed by viewModel.dependencyInfoBarDismissed.collectAsState()
    GnomeFocusView(
        onEvent = viewModel::handleEvent,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex(),
        dependencyInfoBarDismissed = dependencyInfoBarDismissed
    )
}

@Composable
fun GnomeFocusView(
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.GnomeFocus,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    dependencyInfoBarDismissed: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )
        Column(Modifier.weight(1f).fillMaxWidth()) {
            HeaderRow(
                title = stringResource(Res.string.onboarding_gnome_focus_title),
                subtitle = stringResource(Res.string.onboarding_gnome_focus_caption)
            )
            Spacer(Modifier.height(16.dp))
            HyperlinkButton(
                onClick = {
                    uriHandler.openUri("https://extensions.gnome.org/extension/6385/steal-my-focus-window/")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.onboarding_gnome_focus_open_extension))
            }
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
fun GnomeFocusScreenPreview() {
    GnomeFocusView(
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
