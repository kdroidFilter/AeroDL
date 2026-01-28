package io.github.kdroidfilter.ytdlpgui.features.converter

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.MusicNote1
import io.github.composefluent.icons.regular.Video
import io.github.kdroidfilter.ytdlpgui.core.design.components.TrimSlider
import io.github.kdroidfilter.ytdlpgui.core.design.components.formatDuration
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*

@Composable
fun ConverterOptionsScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry
) {
    val appGraph = LocalAppGraph.current
    val viewModel = remember(backStackEntry) {
        appGraph.converterOptionsViewModelFactory.create(backStackEntry.savedStateHandle)
    }
    val state by viewModel.uiState.collectAsState()

    // Handle screen disposal
    DisposableEffect(Unit) {
        onDispose {
            viewModel.handleEvent(ConverterOptionsEvents.ScreenDisposed)
        }
    }

    // Handle navigation to Tasks screen
    LaunchedEffect(state.navigationState) {
        when (state.navigationState) {
            ConverterOptionsNavigationState.NavigateToTasks -> {
                navController.navigate(Destination.MainNavigation.Downloader) {
                    popUpTo(Destination.Converter.Input) {
                        inclusive = true
                    }
                }
                viewModel.handleEvent(ConverterOptionsEvents.OnNavigationConsumed)
            }
            ConverterOptionsNavigationState.None -> {
                // No navigation needed
            }
        }
    }

    ConverterOptionsView(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

@Composable
private fun ConverterOptionsView(
    state: ConverterOptionsState,
    onEvent: (ConverterOptionsEvents) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.isAnalyzing -> LoadingView()
            state.errorMessage != null && state.mediaInfo == null -> ErrorView(state.errorMessage)
            else -> ConversionOptionsContent(state, onEvent)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressRing()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.converter_analyzing),
                style = FluentTheme.typography.body
            )
        }
    }
}

@Composable
private fun ErrorView(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            color = FluentTheme.colors.system.critical,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConversionOptionsContent(
    state: ConverterOptionsState,
    onEvent: (ConverterOptionsEvents) -> Unit
) {
    val mediaInfo = state.mediaInfo ?: return
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                // File info card
                item {
                    FileInfoCard(state, mediaInfo)
                }

                // Format selector (only for video files)
                if (state.showFormatSelector) {
                    item {
                        FormatSelector(state, onEvent)
                    }
                }

                // Video quality selector
                if (state.showVideoOptions) {
                    item {
                        VideoQualitySelector(state, onEvent)
                    }
                }

                // Audio quality selector
                if (state.showAudioOptions) {
                    item {
                        AudioQualitySelector(state, onEvent)
                    }
                }

                // Trim slider
                if (state.showTrimSlider) {
                    item {
                        TrimSlider(
                            trimStartMs = state.trimStartMs,
                            trimEndMs = state.trimEndMs,
                            totalDurationMs = state.totalDurationMs,
                            isTrimmed = state.isTrimmed,
                            onTrimRangeChange = { startMs, endMs ->
                                onEvent(ConverterOptionsEvents.SetTrimRange(startMs, endMs))
                            }
                        )
                    }
                }

                // Error message
                if (state.errorMessage != null) {
                    item {
                        Text(
                            text = state.errorMessage,
                            color = FluentTheme.colors.system.critical,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Convert button
        AccentButton(
            onClick = { onEvent(ConverterOptionsEvents.StartConversion) },
            disabled = !state.canConvert
        ) {
            Text(stringResource(Res.string.converter_start))
        }
    }
}

@Composable
private fun FileInfoCard(
    state: ConverterOptionsState,
    mediaInfo: io.github.kdroidfilter.ffmpeg.model.MediaInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FluentTheme.colors.background.layer.default)
            .border(1.dp, FluentTheme.colors.stroke.control.default, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (state.mediaType == MediaType.VIDEO) Icons.Regular.Video else Icons.Regular.MusicNote1,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.selectedFile?.name ?: "",
                    style = FluentTheme.typography.bodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val infoText = buildString {
                    mediaInfo.duration?.let { append(formatDuration(it.toMillis())) }
                    if (state.mediaType == MediaType.VIDEO) {
                        mediaInfo.videoStreams.firstOrNull()?.let { video ->
                            if (isNotEmpty()) append(" • ")
                            append("${video.width}x${video.height}")
                        }
                    }
                    mediaInfo.audioStreams.firstOrNull()?.let { audio ->
                        audio.sampleRate?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${it / 1000} kHz")
                        }
                    }
                }
                Text(
                    text = infoText,
                    style = FluentTheme.typography.caption,
                    color = FluentTheme.colors.text.text.secondary
                )
            }
        }
    }
}

@Composable
private fun FormatSelector(
    state: ConverterOptionsState,
    onEvent: (ConverterOptionsEvents) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.converter_output_format),
            style = FluentTheme.typography.bodyStrong,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SegmentedControl {
                SegmentedButton(
                    checked = state.outputFormat == OutputFormat.VIDEO_MP4,
                    onCheckedChanged = { onEvent(ConverterOptionsEvents.SetOutputFormat(OutputFormat.VIDEO_MP4)) },
                    position = SegmentedItemPosition.Start,
                    text = { Text(stringResource(Res.string.converter_video)) }
                )
                SegmentedButton(
                    checked = state.outputFormat == OutputFormat.AUDIO_MP3,
                    onCheckedChanged = { onEvent(ConverterOptionsEvents.SetOutputFormat(OutputFormat.AUDIO_MP3)) },
                    position = SegmentedItemPosition.End,
                    text = { Text(stringResource(Res.string.converter_audio)) }
                )
            }
        }
    }
}

@Composable
private fun VideoQualitySelector(
    state: ConverterOptionsState,
    onEvent: (ConverterOptionsEvents) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.converter_video_quality),
            style = FluentTheme.typography.bodyStrong,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoQuality.entries.forEach { quality ->
                SegmentedControl {
                    SegmentedButton(
                        checked = state.selectedVideoQuality == quality,
                        onCheckedChanged = { onEvent(ConverterOptionsEvents.SetVideoQuality(quality)) },
                        position = SegmentedItemPosition.Center,
                        text = { Text(quality.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioQualitySelector(
    state: ConverterOptionsState,
    onEvent: (ConverterOptionsEvents) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.converter_audio_quality),
            style = FluentTheme.typography.bodyStrong,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioQuality.entries.forEach { quality ->
                SegmentedControl {
                    SegmentedButton(
                        checked = state.selectedAudioQuality == quality,
                        onCheckedChanged = { onEvent(ConverterOptionsEvents.SetAudioQuality(quality)) },
                        position = SegmentedItemPosition.Center,
                        text = { Text(quality.displayName) }
                    )
                }
            }
        }
    }
}
