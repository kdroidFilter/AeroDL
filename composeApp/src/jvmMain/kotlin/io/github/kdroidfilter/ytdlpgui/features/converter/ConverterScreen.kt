package io.github.kdroidfilter.ytdlpgui.features.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.*
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import java.awt.datatransfer.DataFlavor
import java.io.File

@Composable
fun ConverterScreen(navController: NavHostController) {
    val viewModel: ConverterViewModel = metroViewModel()
    val state by viewModel.uiState.collectAsState()

    // Keep tray window open while on this screen
    DisposableEffect(Unit) {
        viewModel.onScreenEntered()
        onDispose {
            viewModel.onScreenExited()
        }
    }

    // Navigate to Tasks when conversion is started
    LaunchedEffect(state.navigateToTasks) {
        if (state.navigateToTasks) {
            navController.navigate(io.github.kdroidfilter.ytdlpgui.core.navigation.Destination.MainNavigation.Downloader) {
                popUpTo(io.github.kdroidfilter.ytdlpgui.core.navigation.Destination.Converter.Main) {
                    inclusive = true
                }
            }
            viewModel.onNavigationHandled()
        }
    }

    ConverterView(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

@Composable
private fun ConverterView(
    state: ConverterState,
    onEvent: (ConverterEvents) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.conversionCompleted -> ConversionCompleteView(state, onEvent)
            state.selectedFile != null && state.mediaInfo != null -> ConversionOptionsView(state, onEvent)
            else -> DropZoneView(state, onEvent)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DropZoneView(
    state: ConverterState,
    onEvent: (ConverterEvents) -> Unit
) {
    val borderColor = if (state.isDragging) {
        FluentTheme.colors.fillAccent.default
    } else {
        FluentTheme.colors.stroke.control.default
    }

    val backgroundColor = if (state.isDragging) {
        FluentTheme.colors.fillAccent.default.copy(alpha = 0.1f)
    } else {
        FluentTheme.colors.background.layer.default
    }

    // Create drag and drop target
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                onEvent(ConverterEvents.DragEntered)
            }

            override fun onEnded(event: DragAndDropEvent) {
                onEvent(ConverterEvents.DragExited)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                return try {
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        if (files.isNotEmpty()) {
                            onEvent(ConverterEvents.FilesDropped(files))
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header section
        Text(
            text = stringResource(Res.string.converter_subtitle),
            style = FluentTheme.typography.body,
            color = FluentTheme.colors.text.text.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Spacer(Modifier.weight(1f))

        // Drop zone
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dragAndDropTarget
                )
                .clickable {
                    onEvent(ConverterEvents.OpenFilePicker)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                if (state.isAnalyzing) {
                    ProgressRing()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.converter_analyzing),
                        style = FluentTheme.typography.body
                    )
                } else {
                    Icon(
                        imageVector = Icons.Regular.DocumentAdd,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (state.isDragging) FluentTheme.colors.fillAccent.default else FluentTheme.colors.text.text.secondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.converter_drop_zone),
                        style = FluentTheme.typography.bodyStrong,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.converter_or),
                            style = FluentTheme.typography.body,
                            color = FluentTheme.colors.text.text.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.converter_select_file),
                            style = FluentTheme.typography.body,
                            color = FluentTheme.colors.fillAccent.default
                        )
                    }
                }
            }
        }

        // Supported formats
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.converter_supported_formats),
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.tertiary,
            textAlign = TextAlign.Center
        )

        // Error message
        if (state.errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.errorMessage,
                color = FluentTheme.colors.system.critical,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ConversionOptionsView(
    state: ConverterState,
    onEvent: (ConverterEvents) -> Unit
) {
    val mediaInfo = state.mediaInfo ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // File info card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(FluentTheme.colors.background.layer.default)
                .border(1.dp, FluentTheme.colors.stroke.control.default, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
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

        Spacer(Modifier.height(24.dp))

        // Format selector (only for video files)
        if (state.showFormatSelector) {
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
                        onCheckedChanged = { onEvent(ConverterEvents.SetOutputFormat(OutputFormat.VIDEO_MP4)) },
                        position = SegmentedItemPosition.Start,
                        text = { Text(stringResource(Res.string.converter_video)) }
                    )
                    SegmentedButton(
                        checked = state.outputFormat == OutputFormat.AUDIO_MP3,
                        onCheckedChanged = { onEvent(ConverterEvents.SetOutputFormat(OutputFormat.AUDIO_MP3)) },
                        position = SegmentedItemPosition.End,
                        text = { Text(stringResource(Res.string.converter_audio)) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Video quality selector
        if (state.showVideoOptions) {
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
                            onCheckedChanged = { onEvent(ConverterEvents.SetVideoQuality(quality)) },
                            position = SegmentedItemPosition.Center,
                            text = { Text(quality.displayName) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Audio quality selector
        if (state.showAudioOptions) {
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
                            onCheckedChanged = { onEvent(ConverterEvents.SetAudioQuality(quality)) },
                            position = SegmentedItemPosition.Center,
                            text = { Text(quality.displayName) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Error message
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = FluentTheme.colors.system.critical,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))

        // Progress or Convert button
        if (state.isConverting) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.conversionProgress != null) {
                    ProgressBar(
                        progress = state.conversionProgress,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${(state.conversionProgress * 100).toInt()}%",
                        style = FluentTheme.typography.body
                    )
                } else {
                    ProgressRing()
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.converter_converting),
                    style = FluentTheme.typography.body
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onEvent(ConverterEvents.CancelConversion) }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onEvent(ConverterEvents.Reset) }
                ) {
                    Text(stringResource(Res.string.converter_reset))
                }
                AccentButton(
                    onClick = { onEvent(ConverterEvents.StartConversion) },
                    disabled = !state.canConvert
                ) {
                    Text(stringResource(Res.string.converter_start))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConversionCompleteView(
    state: ConverterState,
    onEvent: (ConverterEvents) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Regular.CheckmarkCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = FluentTheme.colors.system.success
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.converter_success),
            style = FluentTheme.typography.subtitle,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        state.outputFile?.let { file ->
            Text(
                text = file.name,
                style = FluentTheme.typography.body,
                color = FluentTheme.colors.text.text.secondary
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onEvent(ConverterEvents.OpenOutputFolder) }
            ) {
                Icon(
                    imageVector = Icons.Regular.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.converter_open_folder))
            }
            AccentButton(
                onClick = { onEvent(ConverterEvents.Reset) }
            ) {
                Text(stringResource(Res.string.converter_reset))
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
