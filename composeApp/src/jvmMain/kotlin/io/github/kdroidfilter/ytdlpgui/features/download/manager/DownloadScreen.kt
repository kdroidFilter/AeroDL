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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import io.github.kdroidfilter.ytdlpgui.core.design.components.UpdateInfoBar
import io.github.kdroidfilter.ytdlpgui.core.design.components.TerminalView
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppColors
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppContentDialog
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppProgressRing
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSubtleButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTextField
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTooltip
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.TaskType
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository.HistoryItem
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.kdroidfilter.ytdlpgui.di.LocalWindowViewModelStoreOwner
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ytdlpgui.composeapp.generated.resources.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun DownloaderScreen() {
    val viewModel: DownloadViewModel = metroViewModel(
        viewModelStoreOwner = LocalWindowViewModelStoreOwner.current
    )
    val state by viewModel.uiState.collectAsState()
    DownloadView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}


@OptIn(ExperimentalFoundationApi::class)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.hasAnyHistory) {
                            AppTextField(
                                value = state.searchQuery,
                                onValueChange = { onEvent(DownloadEvents.UpdateSearchQuery(it)) },
                                placeholder = stringResource(Res.string.download_search_placeholder),
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                trailingIcon = {
                                    AppIcon(
                                        imageVector = AppIcons.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            )

                            AppTooltip(tooltip = stringResource(Res.string.tooltip_clear_history)) {
                                AppButton(
                                    onClick = { onEvent(DownloadEvents.ClearHistory) },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    AppText(
                                        stringResource(Res.string.download_clear_history),
                                        style = AppTypography.bodyStrong
                                    )
                                    AppIcon(AppIcons.Delete, stringResource(Res.string.download_clear_history))
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
                        color = AppColors.controlSecondary,
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
                            AppTooltip(tooltip = stringResource(Res.string.tooltip_open_directory)) {
                                AppButton(onClick = { onEvent(DownloadEvents.OpenDirectory(h.id)) }) {
                                    AppIcon(AppIcons.Folder, stringResource(Res.string.open_directory))
                                }
                            }
                        } else {
                            AppTooltip(tooltip = stringResource(Res.string.tooltip_directory_unavailable)) {
                                // Disabled/placeholder action when directory no longer exists
                                AppButton(
                                    enabled = false,
                                    onClick = { /* no-op: directory unavailable */ }) {
                                    AppIcon(
                                        AppIcons.FolderProhibited,
                                        stringResource(Res.string.directory_unavailable),
                                        tint = AppColors.critical
                                    )
                                }
                            }
                        }

                        AppTooltip(tooltip = stringResource(Res.string.tooltip_delete_element)) {
                            AppButton(onClick = { onEvent(DownloadEvents.DeleteHistory(h.id)) }) {
                                AppIcon(AppIcons.Delete, stringResource(Res.string.delete_element))
                            }
                        }
                    })
                    Divider(
                        color = AppColors.controlSecondary,
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

@Composable
private fun ErrorDialog(
    errorItem: DownloadManager.DownloadItem,
    onDismiss: () -> Unit
) {
    AppContentDialog(
        title = stringResource(Res.string.download_error_title),
        visible = true,
        primaryButtonText = stringResource(Res.string.ok),
        onButtonClick = { onDismiss() },
        content = {
            TerminalView(
                text = errorItem.message ?: stringResource(Res.string.unknown_error)
            )
        }
    )
}

// TerminalView moved to core.design.components.TerminalView for reuse

@Composable
private fun NoDownloads() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(stringResource(Res.string.no_downloads))
    }
}

@Composable
private fun HistoryThumbnail(h: HistoryItem) {
    Column(
        modifier = Modifier.width(88.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            // Check if this is a conversion (no videoInfo means it's a local file conversion)
            val isConversion = h.videoInfo == null

            if (isConversion) {
                // Show generic icon for conversions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.backgroundDefault),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        imageVector = if (h.isAudio) AppIcons.MusicNote else AppIcons.Video,
                        contentDescription = stringResource(Res.string.task_type_conversion),
                        modifier = Modifier.size(36.dp),
                        tint = AppColors.textSecondary
                    )
                }
            } else {
                // Show YouTube thumbnail for downloads
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
            }

            val overlay = when {
                h.isSplit -> "Split"
                h.isAudio -> "MP3"
                isConversion -> "MP4"
                else -> h.presetHeight?.let { "${it}P" } ?: ""
            }
            AppText(
                overlay,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 2.dp),
                style = AppTypography.caption,
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
            AppText(h.videoInfo?.title ?: h.url, maxLines = 3, overflow = TextOverflow.Ellipsis)
            AppText(whenStr, style = AppTypography.caption)
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
            when (item.taskType) {
                TaskType.DOWNLOAD -> {
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
                }
                TaskType.CONVERSION -> {
                    // Show video or audio icon based on output format
                    val isAudioConversion = item.outputFormat?.uppercase() in listOf("MP3", "AAC", "FLAC", "WAV", "OGG", "M4A", "OPUS")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.backgroundDefault),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            imageVector = if (isAudioConversion) AppIcons.MusicNote else AppIcons.Video,
                            contentDescription = stringResource(Res.string.task_type_conversion),
                            modifier = Modifier.size(36.dp),
                            tint = AppColors.textSecondary
                        )
                    }
                }
            }
            val overlay = when (item.taskType) {
                TaskType.DOWNLOAD -> item.preset?.height?.let { "${it}P" } ?: "MP3"
                TaskType.CONVERSION -> item.outputFormat ?: "Convert"
            }
            AppText(
                overlay,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Black).padding(horizontal = 4.dp, vertical = 2.dp),
                style = AppTypography.caption,
                color = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
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
            AppText(item.displayName, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val statusText = when (item.status) {
                DownloadManager.DownloadItem.Status.Pending -> stringResource(Res.string.status_pending)
                DownloadManager.DownloadItem.Status.Running -> {
                    if (item.taskType == TaskType.CONVERSION) {
                        stringResource(Res.string.status_converting)
                    } else {
                        stringResource(Res.string.status_running)
                    }
                }
                DownloadManager.DownloadItem.Status.Completed -> stringResource(Res.string.status_completed)
                DownloadManager.DownloadItem.Status.Failed -> stringResource(Res.string.status_failed)
                DownloadManager.DownloadItem.Status.Cancelled -> stringResource(Res.string.status_cancelled)
            }
            AppText(statusText, style = AppTypography.caption)
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (item.status) {
                DownloadManager.DownloadItem.Status.Failed -> {
                    // Error details button
                    AppTooltip(tooltip = stringResource(Res.string.view_error_details)) {
                        AppSubtleButton(
                            onClick = { onShowError(item.id) },
                            modifier = Modifier.size(27.dp)
                        ) {
                            AppIcon(
                                AppIcons.ErrorCircle,
                                stringResource(Res.string.view_error_details),
                                tint = AppColors.critical
                            )
                        }
                    }
                    // Dismiss/remove from list button
                    AppTooltip(tooltip = stringResource(Res.string.tooltip_remove_from_list)) {
                        AppSubtleButton(
                            onClick = { onDismissFailed(item.id) },
                            modifier = Modifier.size(27.dp)
                        ) {
                            AppIcon(
                                AppIcons.Close,
                                stringResource(Res.string.remove_from_list)
                            )
                        }
                    }
                }

                else -> {
                    // Show progress ring for running/pending downloads
                    val progressFraction = (item.progress.coerceIn(0f, 100f)) / 100f
                    val percent = (progressFraction * 100f).roundToInt()
                    val speedText = item.speedBytesPerSec?.let { humanizeSpeedLocalized(it) }
                    var hovered by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.width(72.dp), horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier.size(27.dp)
                                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                                .onPointerEvent(PointerEventType.Exit) { hovered = false },
                            contentAlignment = Alignment.Center
                        ) {
                            // Always show the progress ring
                            AppProgressRing(progress = progressFraction, modifier = Modifier.fillMaxSize())

                            if (!hovered) {
                                // Show percentage text centered in the ring
                                AppText(
                                    "${percent}%",
                                    style = AppTypography.caption,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.offset(x = (-1).dp)
                                )
                            } else {
                                // On hover, show dismiss icon overlaid on the ring
                                AppTooltip(tooltip = stringResource(Res.string.cancel)) {
                                    AppSubtleButton(
                                        onClick = { onCancel(item.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        AppIcon(
                                            AppIcons.Close,
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
                            val speedLine =
                                if (item.status == DownloadManager.DownloadItem.Status.Running) speedText else null
                            AppText(
                                text = speedLine ?: " ",
                                style = AppTypography.caption,
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

// Localized humanizer for bytes per second in binary units
@Composable
private fun humanizeSpeedLocalized(bps: Long): String {
    var value = bps.toDouble()
    var idx = 0
    while (value >= 1024.0 && idx < 4) { // up to TiB/s
        value /= 1024.0
        idx++
    }
    val unit = when (idx) {
        0 -> stringResource(Res.string.unit_bps)
        1 -> stringResource(Res.string.unit_kibps)
        2 -> stringResource(Res.string.unit_mibps)
        3 -> stringResource(Res.string.unit_gibps)
        else -> stringResource(Res.string.unit_tibps)
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, unit)
}
