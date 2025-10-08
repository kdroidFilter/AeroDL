package io.github.kdroidfilter.ytdlpgui.features.onboarding.nocheckcert

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
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.component.CheckBox

import io.github.composefluent.icons.regular.LockShield
import io.github.composefluent.icons.regular.Warning
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
import ytdlpgui.composeapp.generated.resources.onboarding_filtered_network_detected_title
import ytdlpgui.composeapp.generated.resources.onboarding_filtered_network_detected_message
import ytdlpgui.composeapp.generated.resources.onboarding_filtered_network_accept
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun NoCheckCertScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state = collectNoCheckCertState(viewModel)
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    NoCheckCertView(
        state = state,
        onEvent = viewModel::onEvents,
        currentStep = currentStep,
        initState = initState,
        totalSteps = viewModel.getTotalSteps(),
        currentStepIndex = viewModel.getCurrentStepIndex()
    )
}

@Composable
fun NoCheckCertView(
    state: NoCheckCertState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.NoCheckCert,
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
            // Warning icon and title
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Regular.Warning, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.onboarding_filtered_network_detected_title),
                    style = FluentTheme.typography.subtitle
                )
            }

            // Message explaining the situation
            Text(
                text = stringResource(Res.string.onboarding_filtered_network_detected_message),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Checkbox to accept
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                CheckBox(
                    state.noCheckCertificate,
                    onCheckStateChange = { onEvent(OnboardingEvents.OnSetNoCheckCertificate(it)) }
                )

                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.onboarding_filtered_network_accept))
            }
        }
        if (initState != null) {
            DependencyInfoBar(initState)
        }
        NavigationRow(
            onNext = { onEvent(OnboardingEvents.OnNext) },
            onPrevious = { onEvent(OnboardingEvents.OnPrevious) },
            nextEnabled = state.noCheckCertificate  // Disable Next until user accepts
        )
    }
}

@Preview
@Composable
fun NoCheckCertScreenPreviewEnabled() {
    NoCheckCertView(
        state = NoCheckCertState.enabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun NoCheckCertScreenPreviewDisabled() {
    NoCheckCertView(
        state = NoCheckCertState.disabledState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
