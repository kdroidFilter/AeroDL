package io.github.kdroidfilter.ytdlpgui.features.screens.download.singledownload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.FlyoutPlacement
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListItemDefaults
import io.github.composefluent.component.ListItemSelectionType
import io.github.composefluent.component.MenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem
import io.github.composefluent.component.MenuFlyoutSeparator
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.SegmentedButton
import io.github.composefluent.component.SegmentedControl
import io.github.composefluent.component.SegmentedItemPosition
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.*
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ytdlpgui.composeapp.generated.resources.*
import java.time.Duration
import java.util.*

@Composable
fun SingleDownloadScreen() {
    val viewModel = koinViewModel<SingleDownloadViewModel>()
    val state = collectSingleDownloadState(viewModel)
    SingleDownloadView(
        state = state,
        onEvent = viewModel::onEvents,
    )
}

@Composable
fun SingleDownloadView(
    state: SingleDownloadState,
    onEvent: (SingleDownloadEvents) -> Unit,
) {
    val videoPlayerState = rememberVideoPlayerState()
    if (state.isLoading) Loader()
    else if (state.errorMessage != null) ErrorBox(state.errorMessage)
    else SingleVideoDownloadView(
        videoPlayerState = videoPlayerState,
        videoInfo = state.videoInfo,
        availablePresets = state.availablePresets,
        selectedPreset = state.selectedPreset,
        availableSubtitleLanguages = state.availableSubtitleLanguages,
        selectedSubtitles = state.selectedSubtitles,
        onSelectPreset = { onEvent(SingleDownloadEvents.SelectPreset(it)) },
        onToggleSubtitle = { onEvent(SingleDownloadEvents.ToggleSubtitle(it)) },
        onClearSubtitles = { onEvent(SingleDownloadEvents.ClearSubtitles) },
        onStartDownload = { onEvent(SingleDownloadEvents.StartDownload) },
        onStartAudioDownload = { onEvent(SingleDownloadEvents.StartAudioDownload) }
    )

}

@Composable
private fun Loader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ProgressRing()
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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SingleVideoDownloadView(
    videoPlayerState: VideoPlayerState,
    videoInfo: VideoInfo?,
    availablePresets: List<YtDlpWrapper.Preset>,
    selectedPreset: YtDlpWrapper.Preset?,
    availableSubtitleLanguages: List<String>,
    selectedSubtitles: List<String>,
    onSelectPreset: (YtDlpWrapper.Preset) -> Unit,
    onToggleSubtitle: (String) -> Unit,
    onClearSubtitles: () -> Unit,
    onStartDownload: () -> Unit,
    onStartAudioDownload: () -> Unit
) {
    var selectedFormatIndex by remember { mutableStateOf(0) } // 0 = Vidéo, 1 = Audio

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fillMaxHeight()
        ) {
            VideoTitle(videoInfo)
            Spacer(Modifier.height(8.dp))

            VideoPlayer(
                thumbnailUrl = videoInfo?.thumbnail,
                duration = videoInfo?.duration,
                videoPlayerState = videoPlayerState,
                videoInfo = videoInfo
            )
            Spacer(Modifier.height(16.dp))

            VideoDescription(videoInfo = videoInfo)

            FormatSelector(
                selectedFormatIndex = selectedFormatIndex,
                onFormatSelected = { selectedFormatIndex = it }
            )
            Spacer(Modifier.height(16.dp))

            if (availablePresets.isNotEmpty()) {
                // Afficher les options vidéo seulement si "Vidéo" est sélectionné
                if (selectedFormatIndex == 0) {
                    // Sélecteur de résolution avec SegmentedControl
                    Text(text = stringResource(Res.string.single_formats))
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SegmentedControl {
                            availablePresets.forEachIndexed { index, preset ->
                                SegmentedButton(
                                    checked = preset == selectedPreset,
                                    onCheckedChanged = { onSelectPreset(preset) },
                                    position = when (index) {
                                        0 -> SegmentedItemPosition.Start
                                        availablePresets.lastIndex -> SegmentedItemPosition.End
                                        else -> SegmentedItemPosition.Center
                                    },
                                    text = { Text("${preset.height}p") }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Sélecteur de sous-titres avec MenuFlyoutContainer
                    var resetKey by remember { mutableStateOf(0) }
                    val sortedLanguages = remember(resetKey, availableSubtitleLanguages) {
                        availableSubtitleLanguages.sortedBy {
                            languageCodeToDisplayName(it).lowercase(Locale.getDefault())
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(Res.string.single_subtitles))

                        MenuFlyoutContainer(
                            flyout = {
                                MenuFlyoutItem(
                                    onClick = {
                                        onClearSubtitles()
                                        resetKey += 1
                                        isFlyoutVisible = false
                                    },
                                    text = { Text(stringResource(Res.string.single_no_subtitle)) }
                                )
                                MenuFlyoutSeparator()
                                sortedLanguages.forEach { lang ->
                                    val isSelected = selectedSubtitles.contains(lang)
                                    val displayName = languageCodeToDisplayName(lang)
                                    MenuFlyoutItem(
                                        selected = isSelected,
                                        onSelectedChanged = {
                                            onToggleSubtitle(lang)
                                        },
                                        selectionType = ListItemSelectionType.Check,
                                        colors = ListItemDefaults.defaultListItemColors(),
                                        text = { Text(displayName) }
                                    )
                                }
                            },
                            content = {
                                Button(
                                    onClick = { isFlyoutVisible = !isFlyoutVisible },
                                    content = {
                                        Text(
                                            if (selectedSubtitles.isEmpty())
                                                stringResource(Res.string.single_no_subtitle)
                                            else
                                                selectedSubtitles.joinToString(", ") { languageCodeToDisplayName(it) }
                                        )
                                    }
                                )
                            },
                            adaptivePlacement = true,
                            placement = FlyoutPlacement.Bottom
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Bouton de téléchargement centré
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {

                    AccentButton(onClick = if (selectedFormatIndex == 0) onStartDownload else onStartAudioDownload) {
                        Text(stringResource(Res.string.download))
                    }

                }
                Spacer(Modifier.height(16.dp))
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
        )
    }
}

@Composable
private fun FormatSelector(
    selectedFormatIndex: Int,
    onFormatSelected: (Int) -> Unit
) {
    val videoLabel = stringResource(Res.string.download_type_video)
    val audioLabel = stringResource(Res.string.download_type_audio)
    val options = listOf(
        videoLabel to Icons.Regular.Video,
        audioLabel to Icons.Regular.MusicNote2
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Regular.FilmstripPlay,
                contentDescription = stringResource(Res.string.cd_filmstrip_play)
            )
            Text(stringResource(Res.string.single_choose_format))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SegmentedControl {
                options.forEachIndexed { index, (label, icon) ->
                    SegmentedButton(
                        checked = index == selectedFormatIndex,
                        onCheckedChanged = { onFormatSelected(index) },
                        position = when (index) {
                            0 -> SegmentedItemPosition.Start
                            options.lastIndex -> SegmentedItemPosition.End
                            else -> SegmentedItemPosition.Center
                        },
                        text = { Text(label) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoTitle(videoInfo: VideoInfo?) {
    Text(
        text = videoInfo?.title.orEmpty(),
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VideoDescription(videoInfo: VideoInfo?) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    if (videoInfo?.description?.isNotEmpty() == true) {
        var expanded by remember { mutableStateOf(false) }
        val degrees by animateFloatAsState(if (expanded) -90f else 90f)

        Column {
            Row(
                modifier = Modifier
                    .clip(FluentTheme.shapes.control)
                    .clickable(indication = null, interactionSource = null) { expanded = !expanded }
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Regular.Textbox,
                        contentDescription = stringResource(Res.string.cd_textbox)
                    )
                    Text(stringResource(Res.string.single_description), style = FluentTheme.typography.body)
                }
                Icon(
                    if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(degrees),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntSize.VisibilityThreshold
                    )
                ),
                exit = shrinkVertically()
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        videoInfo.description.orEmpty(),
                        style = FluentTheme.typography.body
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DurationChip(text: String, modifier: Modifier = Modifier) {
    if (text.isEmpty()) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp, color = Color.White)
    }
}

private fun formatDuration(d: Duration?): String {
    if (d == null) return ""
    val totalSec = d.seconds.coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun languageCodeToDisplayName(langCode: String, targetLocale: Locale = Locale.getDefault()): String {
    val deprecated = mapOf(
        "iw" to "he",
        "in" to "id",
        "ji" to "yi"
    )
    val trimmed = langCode.trim()
    if (trimmed.isEmpty()) return trimmed

    val parts = trimmed.replace('_', '-').split('-', limit = 3).toMutableList()
    if (parts.isNotEmpty()) {
        val primary = parts[0].lowercase()
        parts[0] = deprecated[primary] ?: primary
    }
    val normalizedTag = parts.joinToString("-")

    val locale = Locale.forLanguageTag(normalizedTag)
    val display = locale.getDisplayLanguage(targetLocale).ifEmpty { trimmed }
    return display
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun VideoPlayer(
    thumbnailUrl: String?,
    duration: Duration?,
    videoPlayerState: VideoPlayerState,
    videoInfo: VideoInfo?
) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!videoPlayerState.hasMedia) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = stringResource(Res.string.thumbnail_content_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    imageVector = Icons.Default.Play,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clickable(
                        onClick = {
                            if (videoPlayerState.hasMedia) videoPlayerState.play()
                            else videoInfo?.directUrl?.let { videoPlayerState.openUri(it) }
                        }
                    )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x66000000)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                DurationChip(
                    text = formatDuration(duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            } else {
                VideoPlayerSurface(videoPlayerState, modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (videoPlayerState.isLoading) {
                            ProgressRing(modifier = Modifier.size(48.dp))
                        }
                        if (isHovered) {
                            if (!videoPlayerState.isPlaying) {
                                IconButton({ videoPlayerState.play() }) {
                                    Icon(
                                        imageVector = Icons.Default.Play,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White
                                    )
                                }
                            } else {
                                IconButton({ videoPlayerState.pause() }) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}