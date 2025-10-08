package io.github.kdroidfilter.ytdlpgui.features.onboarding.parallel

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
import io.github.composefluent.icons.filled.TopSpeed
import io.github.kdroidfilter.ytdlpgui.features.onboarding.HeaderRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.NavigationRow
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingEvents
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingProgress
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingStep
import io.github.kdroidfilter.ytdlpgui.features.onboarding.OnboardingViewModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.onboarding_parallel_selected
import ytdlpgui.composeapp.generated.resources.settings_parallel_downloads_caption
import ytdlpgui.composeapp.generated.resources.settings_parallel_downloads_title
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun ParallelScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state = collectParallelState(viewModel)
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    ParallelView(
        state = state,
        onEvent = viewModel::onEvents,
        currentStep = currentStep,
        initState = initState
    )
}

@Composable
fun ParallelView(
    state: ParallelState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.Parallel,
    initState: io.github.kdroidfilter.ytdlpgui.features.init.InitState? = null,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OnboardingProgress(step = currentStep, initState = initState)
        Column(Modifier.weight(1f).fillMaxWidth()) {
            HeaderRow(
                title = stringResource(Res.string.settings_parallel_downloads_title),
                subtitle = stringResource(Res.string.settings_parallel_downloads_caption)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { count ->
                    Button(onClick = { onEvent(OnboardingEvents.OnSetParallelDownloads(count)) }) {
                        Text(count.toString())
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TopSpeed, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.onboarding_parallel_selected, state.parallelDownloads))
            }
        }
        NavigationRow(
            onNext = { onEvent(OnboardingEvents.OnNext) },
            onSkip = { onEvent(OnboardingEvents.OnSkip) }
        )
    }
}

@Preview
@Composable
fun ParallelScreenPreview() {
    ParallelView(
        state = ParallelState.defaultState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}
