package io.github.kdroidfilter.ytdlpgui.features.onboarding.downloaddir

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
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.FolderOpen
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
import ytdlpgui.composeapp.generated.resources.settings_download_dir_caption
import ytdlpgui.composeapp.generated.resources.settings_download_dir_not_set
import ytdlpgui.composeapp.generated.resources.settings_download_dir_pick_title
import ytdlpgui.composeapp.generated.resources.settings_download_dir_title
import ytdlpgui.composeapp.generated.resources.settings_select
import io.github.kdroidfilter.ytdlpgui.features.init.InitState

@Composable
fun DownloadDirScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val downloadDirPath by viewModel.downloadDirPath.collectAsState()
    val state = DownloadDirState(
        downloadDirPath = downloadDirPath
    )
    val currentStep by viewModel.currentStep.collectAsState()
    val initState by viewModel.initState.collectAsState()
    val dependencyInfoBarDismissed by viewModel.dependencyInfoBarDismissed.collectAsState()
    DownloadDirView(
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
fun DownloadDirView(
    state: DownloadDirState,
    onEvent: (OnboardingEvents) -> Unit,
    currentStep: OnboardingStep = OnboardingStep.DownloadDir,
    initState: InitState? = null,
    totalSteps: Int? = null,
    currentStepIndex: Int? = null,
    dependencyInfoBarDismissed: Boolean = false,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OnboardingProgress(
            step = currentStep,
            initState = initState,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex
        )
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HeaderRow(
                title = stringResource(Res.string.settings_download_dir_title),
                subtitle = stringResource(Res.string.settings_download_dir_caption)
            )
            Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(48.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                val directoryLabel = state.downloadDirPath.ifBlank {
                    stringResource(Res.string.settings_download_dir_not_set)
                }
                Text(directoryLabel)
            }
            Spacer(Modifier.height(12.dp))
            val pickFolderTitle = stringResource(Res.string.settings_download_dir_pick_title)
            AccentButton(onClick = { onEvent(OnboardingEvents.OnPickDownloadDir(pickFolderTitle)) }) {
                Text(stringResource(Res.string.settings_select))
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
            onPrevious = { onEvent(OnboardingEvents.OnPrevious) },
            nextEnabled = state.downloadDirPath.isNotBlank()  // Disable Next until directory selected
        )
    }
}

@Preview
@Composable
fun DownloadDirScreenPreviewEmpty() {
    DownloadDirView(
        state = DownloadDirState.emptyState,
        onEvent = {},
        initState = InitState(downloadingYtDlp = true)
    )
}

@Preview
@Composable
fun DownloadDirScreenPreviewConfigured() {
    DownloadDirView(
        state = DownloadDirState.configuredState,
        onEvent = {},
        initState = InitState(initCompleted = true)
    )
}
