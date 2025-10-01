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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

        // Section: In-progress downloads
        val inProgress = state.items.filter { it.status == DownloadManager.DownloadItem.Status.Running || it.status == DownloadManager.DownloadItem.Status.Pending }
        Text("Téléchargements en cours")
        Spacer(Modifier.height(8.dp))
        if (inProgress.isEmpty()) {
            Text("Aucun téléchargement en cours")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                inProgress.forEach { item ->
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

        Spacer(Modifier.height(16.dp))

        // Section: History
        Text("Historique")
        Spacer(Modifier.height(8.dp))
        if (state.history.isEmpty()) {
            Text("Aucun téléchargement précédent")
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.history.forEach { h ->
                    Column(Modifier.fillMaxWidth()) {
                        Text(h.videoInfo?.title ?: h.url)
                        if (h.videoInfo?.title != null) {
                            Text(h.url)
                        }
                        val typeLabel = if (h.isAudio) stringResource(Res.string.download_type_audio) else stringResource(Res.string.download_type_video)
                        val typeAndPreset = if (!h.isAudio && h.presetHeight != null) "$typeLabel • ${h.presetHeight}p" else typeLabel
                        Text(typeAndPreset)
                        val whenStr = formatter.format(Instant.ofEpochMilli(h.createdAt))
                        val whereStr = h.outputPath ?: ""
                        Text("$whenStr • $whereStr")
                    }
                }
            }
        }
    }
}