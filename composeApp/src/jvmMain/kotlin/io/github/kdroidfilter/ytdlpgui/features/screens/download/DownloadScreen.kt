package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.composefluent.component.Button
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadScreen() {
    val viewModel = koinViewModel<DownloadViewModel>()
    val state = collectDownloadState(viewModel)
    DownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun DownloadView(
    state: DownloadState,
    onEvent: (DownloadEvents) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(Res.string.history_screen_title))
        Spacer(Modifier.height(12.dp))
        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No downloads yet")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.videoInfo?.title ?: item.url)
                            if (item.videoInfo?.title != null) {
                                Text(item.url)
                            }
                            val typeLabel = if (item.preset != null) stringResource(Res.string.download_type_video) else stringResource(Res.string.download_type_audio)
                            val typeAndPreset = if (item.preset != null) "$typeLabel • ${item.preset.height}p" else typeLabel
                            Text(typeAndPreset)
                            val statusText = when (item.status) {
                                DownloadManager.DownloadItem.Status.Pending -> "Pending"
                                DownloadManager.DownloadItem.Status.Running -> "Running"
                                DownloadManager.DownloadItem.Status.Completed -> "Completed"
                                DownloadManager.DownloadItem.Status.Failed -> "Failed"
                                DownloadManager.DownloadItem.Status.Cancelled -> "Cancelled"
                            }
                            Text("$statusText • ${"%.1f".format(item.progress)}%")
                            item.message?.let { Text(it) }
                        }
                        if (item.status == DownloadManager.DownloadItem.Status.Running) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProgressRing(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { onEvent(DownloadEvents.Cancel(item.id)) }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}