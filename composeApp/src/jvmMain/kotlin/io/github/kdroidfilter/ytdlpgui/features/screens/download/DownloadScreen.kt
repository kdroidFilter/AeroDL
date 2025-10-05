package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.Stroke
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text
import io.github.composefluent.component.TooltipBox
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Folder
import io.github.composefluent.icons.regular.Dismiss
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository.HistoryItem
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DownloadScreen() {
    val viewModel = koinViewModel<DownloadViewModel>()
    val state = collectDownloadState(viewModel)
    DownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalFluentApi::class)
@Composable
fun DownloadView(
    state: DownloadState,
    onEvent: (DownloadEvents) -> Unit,
) {
    val listState = rememberLazyListState()

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.history.isNotEmpty()) {
                        TooltipBox(tooltip = {
                            Text(stringResource(Res.string.tooltip_clear_history))
                        }) {
                            Button(onClick = { onEvent(DownloadEvents.ClearHistory) }, modifier = Modifier.padding(bottom = 8.dp)) {
                                Text(stringResource(Res.string.download_clear_history), style = FluentTheme.typography.bodyStrong)
                                Icon(Icons.Default.Delete, stringResource(Res.string.download_clear_history))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Section: In-progress downloads
            val inProgress =
                state.items.filter { it.status == DownloadManager.DownloadItem.Status.Running || it.status == DownloadManager.DownloadItem.Status.Pending }

            items(inProgress) { item ->
                InProgressRow(item, onCancel = { id -> onEvent(DownloadEvents.Cancel(id)) })
                Divider(
                    color = FluentTheme.colors.control.secondary,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            // Section: History

            if (state.history.isEmpty() and inProgress.isEmpty()) {
                item {
                    NoDownloads()
                }
            }

            items(state.history) { h ->
                HistoryRow(h = h, actions = {
                    TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_open_directory)) }) {
                        Button(iconOnly = true, onClick = { }) {
                            Icon(Icons.Default.Folder, stringResource(Res.string.open_directory))
                        }
                    }

                    TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_delete_element)) }) {
                        Button(iconOnly = true, onClick = { onEvent(DownloadEvents.DeleteHistory(h.id)) }) {
                            Icon(Icons.Default.Delete, stringResource(Res.string.delete_element))
                        }
                    }
                })
                Divider(
                    color = FluentTheme.colors.control.secondary,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
        )
    }
}

@Composable
private fun NoDownloads() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(Res.string.no_downloads))
    }
}

@Composable
private fun HistoryThumbnail(h: HistoryItem) {
    Column(
        modifier = Modifier.width(88.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            val thumbUrl = h.videoInfo?.let {
                YouTubeThumbnailHelper.getThumbnailUrl(
                    it.id,
                    YouTubeThumbnailHelper.ThumbnailQuality.MEDIUM
                )
            }
            AsyncImage(
                model = thumbUrl,
                contentDescription = stringResource(Res.string.thumbnail_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val overlay = if (h.isAudio) "MP3" else h.presetHeight?.let { "${it}P" } ?: ""
            Text(
                overlay,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 2.dp),
                style = FluentTheme.typography.caption,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    h: HistoryItem,
    actions: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryThumbnail(h)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
        val whenStr = formatter.format(Instant.ofEpochMilli(h.createdAt))

        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(h.videoInfo?.title ?: h.url, maxLines = 3)
            Text(whenStr, style = FluentTheme.typography.caption)
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            actions()
        }
    }
}

@Composable
private fun InProgressThumbnail(item: DownloadManager.DownloadItem) {
    Column(
        modifier = Modifier.width(88.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            val thumbUrl = item.videoInfo?.let {
                YouTubeThumbnailHelper.getThumbnailUrl(
                    it.id,
                    YouTubeThumbnailHelper.ThumbnailQuality.MEDIUM
                )
            }
            AsyncImage(
                model = thumbUrl,
                contentDescription = stringResource(Res.string.thumbnail_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            val overlay = item.preset?.height?.let { "${it}P" } ?: "MP3"
            Text(
                overlay,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 2.dp),
                style = FluentTheme.typography.caption,
                color = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFluentApi::class, ExperimentalFoundationApi::class)
@Composable
private fun InProgressRow(item: DownloadManager.DownloadItem, onCancel: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        InProgressThumbnail(item)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(item.videoInfo?.title ?: item.url, maxLines = 3)
            val statusText = when (item.status) {
                DownloadManager.DownloadItem.Status.Pending -> stringResource(Res.string.status_pending)
                DownloadManager.DownloadItem.Status.Running -> stringResource(Res.string.status_running)
                DownloadManager.DownloadItem.Status.Completed -> stringResource(Res.string.status_completed)
                DownloadManager.DownloadItem.Status.Failed -> stringResource(Res.string.status_failed)
                DownloadManager.DownloadItem.Status.Cancelled -> stringResource(Res.string.status_cancelled)
            }
            Text(statusText, style = FluentTheme.typography.caption)
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val progressFraction = (item.progress.coerceIn(0f, 100f)) / 100f
            val percent = (progressFraction * 100f).roundToInt()
            var hovered by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.size(27.dp)
                    .onPointerEvent(PointerEventType.Enter) { hovered = true }
                    .onPointerEvent(PointerEventType.Exit) { hovered = false },
                contentAlignment = Alignment.Center
            ) {
                // Always show the progress ring
                ProgressRing(progress = progressFraction, modifier = Modifier.fillMaxSize().padding(end = 2.dp))

                if (!hovered) {
                    // Show percentage text centered in the ring
                    Text(
                        "${percent}%",
                        style = FluentTheme.typography.caption,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                } else {
                    // On hover, show dismiss icon overlaid on the ring
                    TooltipBox(tooltip = { Text(stringResource(Res.string.cancel)) }) {
                        SubtleButton(
                            iconOnly = true,
                            onClick = { onCancel(item.id) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Dismiss, stringResource(Res.string.cancel), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
