package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.*
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun BulkDownloadScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry
) {
    val appGraph = LocalAppGraph.current
    val viewModel = remember(backStackEntry) {
        appGraph.bulkDownloadViewModelFactory.create(backStackEntry.savedStateHandle)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigationState) {
        when (state.navigationState) {
            is BulkDownloadNavigationState.NavigateToDownloader -> {
                navController.navigate(Destination.MainNavigation.Downloader)
                viewModel.handleEvent(BulkDownloadEvents.OnNavigationConsumed)
            }
            BulkDownloadNavigationState.None -> { }
        }
    }

    BulkDownloadView(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

@Composable
fun BulkDownloadView(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose {
            onEvent(BulkDownloadEvents.ScreenDisposed)
        }
    }

    when {
        state.isLoading -> Loader()
        state.errorMessage != null -> ErrorBox(state.errorMessage)
        state.videos.isEmpty() -> EmptyPlaylist()
        else -> PlaylistContent(state, onEvent)
    }
}

@Composable
private fun Loader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.loading))
        }
    }
}

@Composable
private fun ErrorBox(message: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Regular.ErrorCircle,
            contentDescription = stringResource(Res.string.cd_error_icon),
            modifier = Modifier.size(144.dp),
            tint = FluentTheme.colors.system.critical
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(Res.string.error_fetch_video_info, message),
            color = FluentTheme.colors.system.critical,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun EmptyPlaylist() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Regular.VideoClipMultiple,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = FluentTheme.colors.text.text.secondary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.bulk_no_videos),
            color = FluentTheme.colors.text.text.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaylistContent(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit
) {
    val listState = rememberLazyListState()

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                item {
                    PlaylistHeader(state, onEvent)
                    Spacer(Modifier.height(8.dp))
                }

                // Video items (like DownloadScreen rows)
                items(
                    items = state.videos,
                    key = { it.videoInfo.id }
                ) { videoItem ->
                    VideoRow(
                        item = videoItem,
                        onToggle = { onEvent(BulkDownloadEvents.ToggleVideoSelection(videoItem.videoInfo.id)) }
                    )
                    Divider(
                        color = FluentTheme.colors.control.secondary,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                // Download options (like SingleDownloadScreen)
                item {
                    Spacer(Modifier.height(8.dp))
                    DownloadOptions(state, onEvent)
                    Spacer(Modifier.height(16.dp))
                }

                // Download button
                item {
                    DownloadButton(state, onEvent)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
        )
    }
}

@OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaylistHeader(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit
) {
    Column {
        // Title
        Text(
            text = state.playlistInfo?.title ?: "Playlist",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        // Stats and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.bulk_videos_count, state.totalCount),
                    style = FluentTheme.typography.caption
                )

                if (state.isCheckingAvailability) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${state.checkedCount}/${state.totalCount}",
                            style = FluentTheme.typography.caption
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.bulk_selected_count, state.selectedCount),
                    style = FluentTheme.typography.caption,
                    color = FluentTheme.colors.fillAccent.default,
                    fontWeight = FontWeight.Medium
                )
            }

            // Toggle select all / deselect all
            val allSelected = state.allSelected
            TooltipBox(
                tooltip = {
                    Text(
                        if (allSelected) stringResource(Res.string.bulk_deselect_all)
                        else stringResource(Res.string.bulk_select_all)
                    )
                }
            ) {
                SubtleButton(
                    iconOnly = true,
                    onClick = {
                        if (allSelected) onEvent(BulkDownloadEvents.DeselectAll)
                        else onEvent(BulkDownloadEvents.SelectAll)
                    }
                ) {
                    Icon(
                        if (allSelected) Icons.Regular.SelectAllOff else Icons.Regular.SelectAllOn,
                        if (allSelected) stringResource(Res.string.bulk_deselect_all)
                        else stringResource(Res.string.bulk_select_all)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoRow(
    item: BulkVideoItem,
    onToggle: () -> Unit
) {
    val isEnabled = item.isAvailable && !item.isChecking

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(enabled = isEnabled, onClick = onToggle),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox column
        Box(
            modifier = Modifier.width(32.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (item.isChecking) {
                ProgressRing(modifier = Modifier.size(20.dp))
            } else {
                CheckBox(
                    item.isSelected,
                    onCheckStateChange = { if (isEnabled) onToggle() }
                )
            }
        }

        // Thumbnail (like DownloadScreen)
        VideoThumbnail(item)

        Spacer(Modifier.width(8.dp))

        // Video info
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.videoInfo.title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.videoInfo.uploader?.let { uploader ->
                    Text(
                        text = uploader,
                        style = FluentTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.videoInfo.duration?.let { duration ->
                    Text(
                        text = formatDuration(duration),
                        style = FluentTheme.typography.caption
                    )
                }
                if (!item.isAvailable) {
                    Text(
                        text = stringResource(Res.string.bulk_video_unavailable),
                        style = FluentTheme.typography.caption,
                        color = FluentTheme.colors.system.critical
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(item: BulkVideoItem) {
    Column(
        modifier = Modifier.width(88.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val alpha = if (item.isAvailable) 1f else 0.5f

            AsyncImage(
                model = ImageRequest.Builder(coil3.PlatformContext.INSTANCE)
                    .data(item.videoInfo.thumbnail)
                    .size(88, 72)
                    .build(),
                contentDescription = stringResource(Res.string.thumbnail_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = alpha
            )

            if (!item.isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Regular.ErrorCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadOptions(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit
) {
    val videoLabel = stringResource(Res.string.download_type_video)
    val audioLabel = stringResource(Res.string.download_type_audio)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Format selector (like SingleDownloadScreen)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Regular.FilmstripPlay, contentDescription = null)
                Text(stringResource(Res.string.single_choose_format))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SegmentedControl {
                    SegmentedButton(
                        checked = !state.isAudioMode,
                        onCheckedChanged = { onEvent(BulkDownloadEvents.SetAudioMode(false)) },
                        position = SegmentedItemPosition.Start,
                        text = { Text(videoLabel) },
                        icon = { Icon(Icons.Regular.Video, contentDescription = videoLabel) }
                    )
                    SegmentedButton(
                        checked = state.isAudioMode,
                        onCheckedChanged = { onEvent(BulkDownloadEvents.SetAudioMode(true)) },
                        position = SegmentedItemPosition.End,
                        text = { Text(audioLabel) },
                        icon = { Icon(Icons.Regular.MusicNote2, contentDescription = audioLabel) }
                    )
                }
            }
        }

        // Quality selector (like SingleDownloadScreen)
        if (!state.isAudioMode && state.availablePresets.isNotEmpty()) {
            Text(text = stringResource(Res.string.single_formats))
            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availablePresets.forEach { preset ->
                    SegmentedControl {
                        SegmentedButton(
                            checked = preset == state.selectedPreset,
                            onCheckedChanged = { onEvent(BulkDownloadEvents.SelectPreset(preset)) },
                            position = SegmentedItemPosition.Center,
                            text = { Text("${preset.height}p") }
                        )
                    }
                }
            }
        }

        if (state.isAudioMode && state.availableAudioQualityPresets.isNotEmpty()) {
            Text(text = stringResource(Res.string.single_audio_quality))
            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableAudioQualityPresets.forEach { preset ->
                    SegmentedControl {
                        SegmentedButton(
                            checked = preset == state.selectedAudioQualityPreset,
                            onCheckedChanged = { onEvent(BulkDownloadEvents.SelectAudioQualityPreset(preset)) },
                            position = SegmentedItemPosition.Center,
                            text = { Text(preset.bitrate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit
) {
    val canStart = state.selectedCount > 0 && !state.isStartingDownloads && !state.isCheckingAvailability

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        AccentButton(
            onClick = { if (canStart) onEvent(BulkDownloadEvents.StartDownloads) }
        ) {
            if (state.isStartingDownloads) {
                ProgressRing(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.bulk_starting_downloads))
            } else {
                Icon(Icons.Default.ArrowDownload, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.bulk_download_selected) + " (${state.selectedCount})")
            }
        }
    }
}

private fun formatDuration(d: java.time.Duration): String {
    val totalSec = d.seconds.coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
