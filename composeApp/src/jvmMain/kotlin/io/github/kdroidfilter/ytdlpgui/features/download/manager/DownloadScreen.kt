package io.github.kdroidfilter.ytdlpgui.features.download.manager

import androidx.compose.foundation.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Button
import io.github.composefluent.component.ContentDialog
import io.github.composefluent.component.DialogSize
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text
import io.github.composefluent.component.TooltipBox
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.*
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import io.github.kdroidfilter.ytdlpgui.core.design.components.UpdateInfoBar
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository.HistoryItem
import io.github.kdroidfilter.ytdlpgui.core.design.components.UpdateInfoBar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun DownloaderScreen() {
    val viewModel = koinViewModel<DownloadViewModel>()
    val state by viewModel.uiState.collectAsState()
    DownloadView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

 

@OptIn(ExperimentalFoundationApi::class, ExperimentalFluentApi::class)
@Composable
fun DownloadView(
    state: DownloadState,
    onEvent: (DownloadEvents) -> Unit,
) {
    val listState = rememberLazyListState()

    // Error dialog
    state.errorDialogItem?.let { errorItem ->
        ErrorDialog(
            errorItem = errorItem,
            onDismiss = { onEvent(DownloadEvents.DismissErrorDialog) }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (state.updateAvailable && state.updateVersion != null && state.updateUrl != null) {
                UpdateInfoBar(
                    updateVersion = state.updateVersion,
                    updateBody = state.updateBody,
                    updateUrl = state.updateUrl,
                    onDismiss = { onEvent(DownloadEvents.DismissUpdateInfoBar) },
                    modifier = Modifier
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
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
                            Button(
                                onClick = { onEvent(DownloadEvents.ClearHistory) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.download_clear_history),
                                    style = FluentTheme.typography.bodyStrong
                                )
                                Icon(Icons.Default.Delete, stringResource(Res.string.download_clear_history))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

                // Section: In-progress and failed downloads
            val activeItems =
                state.items.filter {
                    it.status == DownloadManager.DownloadItem.Status.Running ||
                            it.status == DownloadManager.DownloadItem.Status.Pending ||
                            it.status == DownloadManager.DownloadItem.Status.Failed
                }

            items(items = activeItems, key = { it.id }) { item ->
                InProgressRow(
                    item = item,
                    onCancel = { id -> onEvent(DownloadEvents.Cancel(id)) },
                    onShowError = { id -> onEvent(DownloadEvents.ShowErrorDialog(id)) },
                    onDismissFailed = { id -> onEvent(DownloadEvents.DismissFailed(id)) }
                )
                Divider(
                    color = FluentTheme.colors.control.secondary,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

                // Section: History

                if (state.history.isEmpty() and activeItems.isEmpty()) {
                    item {
                        NoDownloads()
                    }
                }

                items(items = state.history, key = { it.id }) { h ->
                HistoryRow(h = h, actions = {
                    val dirAvailable = state.directoryAvailability[h.id] == true
                    if (dirAvailable) {
                        TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_open_directory)) }) {
                            Button(iconOnly = true, onClick = { onEvent(DownloadEvents.OpenDirectory(h.id)) }) {
                                Icon(Icons.Default.Folder, stringResource(Res.string.open_directory))
                            }
                        }
                    } else {
                        TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_directory_unavailable)) }) {
                            // Disabled/placeholder action when directory no longer exists
                            Button(iconOnly = true, disabled = true, onClick = { /* no-op: directory unavailable */ }) {
                                Icon(
                                    Icons.Default.FolderProhibited,
                                    stringResource(Res.string.directory_unavailable),
                                    tint = FluentTheme.colors.system.critical
                                )
                            }
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
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
        )
    }
}

@OptIn(ExperimentalFluentApi::class)
@Composable
private fun ErrorDialog(
    errorItem: DownloadManager.DownloadItem,
    onDismiss: () -> Unit
) {
    ContentDialog(
        title = stringResource(Res.string.download_error_title),
        visible = true,
        size = DialogSize.Min,
        primaryButtonText = stringResource(Res.string.ok),
        onButtonClick = { onDismiss() },
        content = {
            TerminalView(
                text = errorItem.message ?: stringResource(Res.string.unknown_error)
            )
        }
    )
}

@OptIn(ExperimentalFluentApi::class)
@Composable
private fun TerminalView(text: String) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 300.dp)
            .background(
                Color(0xFF1E1E1E), // Dark terminal background
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF3E3E3E),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Terminal header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Error Output",
                    style = FluentTheme.typography.caption,
                    color = Color(0xFFB0B0B0),
                    fontSize = 11.sp
                )

                // Copy button
                @OptIn(ExperimentalFoundationApi::class)
                TooltipBox(tooltip = { Text(stringResource(Res.string.copy_error)) }) {
                    SubtleButton(
                        iconOnly = true,
                        onClick = {
                            clipboardManager.setText(buildAnnotatedString { append(text) })
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Copy,
                            stringResource(Res.string.copy_error),
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Terminal content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text,
                    style = FluentTheme.typography.caption.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFFD4D4D4), // Terminal text color
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
                model = ImageRequest.Builder(coil3.PlatformContext.INSTANCE)
                    .data(thumbUrl)
                    // The row height is 72.dp and thumbnail column width is 88.dp
                    // Provide a fixed request size to avoid decoding oversized bitmaps.
                    .size(88, 72)
                    .build(),
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
            Text(h.videoInfo?.title ?: h.url, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
                model = ImageRequest.Builder(coil3.PlatformContext.INSTANCE)
                    .data(thumbUrl)
                    .size(88, 72)
                    .build(),
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
private fun InProgressRow(
    item: DownloadManager.DownloadItem,
    onCancel: (String) -> Unit,
    onShowError: (String) -> Unit,
    onDismissFailed: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        InProgressThumbnail(item)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(item.videoInfo?.title ?: item.url, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
            when (item.status) {
                DownloadManager.DownloadItem.Status.Failed -> {
                    // Error details button
                    TooltipBox(tooltip = { Text(stringResource(Res.string.view_error_details)) }) {
                        SubtleButton(
                            iconOnly = true,
                            onClick = { onShowError(item.id) },
                            modifier = Modifier.size(27.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorCircle,
                                stringResource(Res.string.view_error_details),
                                tint = FluentTheme.colors.system.critical
                            )
                        }
                    }
                    // Dismiss/remove from list button
                    TooltipBox(tooltip = { Text(stringResource(Res.string.tooltip_remove_from_list)) }) {
                        SubtleButton(
                            iconOnly = true,
                            onClick = { onDismissFailed(item.id) },
                            modifier = Modifier.size(27.dp)
                        ) {
                            Icon(
                                Icons.Default.Dismiss,
                                stringResource(Res.string.remove_from_list)
                            )
                        }
                    }
                }

                else -> {
                    // Show progress ring for running/pending downloads
                    val progressFraction = (item.progress.coerceIn(0f, 100f)) / 100f
                    val percent = (progressFraction * 100f).roundToInt()
                    val speedText = item.speedBytesPerSec?.let { humanizeSpeed(it) }
                    var hovered by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.width(72.dp), horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier.size(27.dp)
                                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                                .onPointerEvent(PointerEventType.Exit) { hovered = false },
                            contentAlignment = Alignment.Center
                        ) {
                            // Always show the progress ring
                            ProgressRing(progress = progressFraction, modifier = Modifier.fillMaxSize())

                            if (!hovered) {
                                // Show percentage text centered in the ring
                                Text(
                                    "${percent}%",
                                    style = FluentTheme.typography.caption,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.offset(x = (-1).dp)
                                )
                            } else {
                                // On hover, show dismiss icon overlaid on the ring
                                TooltipBox(tooltip = { Text(stringResource(Res.string.cancel)) }) {
                                    SubtleButton(
                                        iconOnly = true,
                                        onClick = { onCancel(item.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Dismiss,
                                            stringResource(Res.string.cancel),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Below the ring: stack percent and speed like history action buttons
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                            val speedLine = if (item.status == DownloadManager.DownloadItem.Status.Running) speedText else null
                            Text(
                                text = speedLine?.let { stringResource(Res.string.download_speed, it) } ?: " ",
                                style = FluentTheme.typography.caption,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DownloadScreenPreviewEmpty() {
    DownloadView(state = DownloadState.emptyState, onEvent = {})
}

@Preview
@Composable
fun DownloadScreenPreviewWithInProgress() {
    DownloadView(state = DownloadState.withInProgressState, onEvent = {})
}

@Preview
@Composable
fun DownloadScreenPreviewWithHistory() {
    DownloadView(state = DownloadState.withHistoryState, onEvent = {})
}

@Preview
@Composable
fun DownloadScreenPreviewMixed() {
    DownloadView(state = DownloadState.mixedState, onEvent = {})
}

@Preview
@Composable
fun DownloadScreenPreviewWithError() {
    DownloadView(state = DownloadState.withErrorState, onEvent = {})
}

@Preview
@Composable
fun DownloadScreenPreviewWithErrorDialog() {
    DownloadView(state = DownloadState.withErrorDialogState, onEvent = {})
}

@Preview
@Composable
fun DownloadScreenPreviewWithUpdateInfo() {
    DownloadView(state = DownloadState.withUpdateInfoState, onEvent = {})
}

// Simple humanizer for bytes per second in binary units
private fun humanizeSpeed(bps: Long): String {
    val units = arrayOf("B/s", "KiB/s", "MiB/s", "GiB/s", "TiB/s")
    var value = bps.toDouble()
    var idx = 0
    while (value >= 1024.0 && idx < units.lastIndex) {
        value /= 1024.0
        idx++
    }
    return String.format(Locale.US, "%.1f %s", value, units[idx])
}
