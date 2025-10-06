package io.github.kdroidfilter.ytdlpgui.features.screens.download.singledownload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.onPointerEvent
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
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Text
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.DropDownButton
import io.github.composefluent.component.MenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem
import io.github.composefluent.component.FlyoutPlacement
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ChevronDown
import io.github.composefluent.icons.regular.ChevronLeft
import io.github.composefluent.icons.regular.ChevronRight
import io.github.composefluent.icons.regular.ErrorCircle
import io.github.composefluent.icons.regular.Pause
import io.github.composefluent.icons.regular.Play
import io.github.composefluent.icons.regular.Textbox
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration
import java.util.Locale

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
            contentDescription = "ErrorCircle",
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

/**
 * Main video section: preview on the right (as in the screenshot), title +
 * description on the left.
 */
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            item {
                Text(
                    text = videoInfo?.title.orEmpty(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                VideoPlayer(
                    thumbnailUrl = videoInfo?.thumbnail,
                    duration = videoInfo?.duration,
                    videoPlayerState = videoPlayerState,
                    videoInfo = videoInfo
                )
                Spacer(Modifier.height(16.dp))
            }

            if (videoInfo?.description?.isNotEmpty() == true) {
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val degrees by animateFloatAsState(if (expanded) -90f else 90f)
                    Column {
                        Row(
                            modifier = Modifier.clip(FluentTheme.shapes.control)
                                .clickable(indication = null, interactionSource = null) { expanded = expanded.not() }
                                .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Regular.Textbox,
                                    contentDescription = "Textbox"
                                )
                                Text("Description", style = FluentTheme.typography.body)
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

            item {
                if (availablePresets.isNotEmpty()) {
                    Text(text = stringResource(Res.string.single_formats))
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availablePresets.size) { index ->
                            val preset = availablePresets[index]
                            val label = "${preset.height}p"
                            if (preset == selectedPreset) {
                                AccentButton(onClick = { onSelectPreset(preset) }) { Text(label) }
                            } else {
                                Button(onClick = { onSelectPreset(preset) }) { Text(label) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Subtitles drop-down
                Text(text = stringResource(Res.string.single_subtitles))
                Spacer(Modifier.height(8.dp))
                val subtitleLabel =
                    if (selectedSubtitles.isEmpty()) stringResource(Res.string.single_no_subtitle)
                    else selectedSubtitles.joinToString(", ") { languageCodeToDisplayName(it) }
                MenuFlyoutContainer(
                    flyout = {
                        // None option (clear all)
                        MenuFlyoutItem(
                            text = { Text(stringResource(Res.string.single_no_subtitle)) },
                            onClick = {
                                onClearSubtitles()
                                isFlyoutVisible = false
                            },
                        )
                        // Languages (toggle selection) - sorted alphabetically by localized display name
                        val sortedLanguages = availableSubtitleLanguages
                            .sortedBy { languageCodeToDisplayName(it).lowercase(Locale.getDefault()) }
                        sortedLanguages.forEach { lang ->
                            val checked = selectedSubtitles.contains(lang)
                            val displayName = languageCodeToDisplayName(lang)
                            val label = (if (checked) "✓ " else "") + displayName
                            MenuFlyoutItem(
                                text = { Text(label) },
                                onClick = {
                                    onToggleSubtitle(lang)
                                    isFlyoutVisible = false
                                }
                            )
                        }
                    },
                    content = {
                        DropDownButton(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                            onClick = { isFlyoutVisible = !isFlyoutVisible },
                            content = {
                                Text(subtitleLabel)
                            },
                        )
                    },
                    adaptivePlacement = true,
                    placement = FlyoutPlacement.Bottom
                )
                Spacer(Modifier.height(16.dp))

                AccentButton(onClick = onStartDownload) {
                    Text(stringResource(Res.string.download))
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onStartAudioDownload) {
                    Text(stringResource(Res.string.download_audio))
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
        )
    }
}

/** Small pill for the duration text. */
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

/** Format java.time.Duration? into H:MM:SS (or MM:SS). */
private fun formatDuration(d: Duration?): String {
    if (d == null) return ""
    val totalSec = d.seconds.coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Convert a BCP-47-like language code (e.g. "en", "en-GB", legacy "iw")
 * into a localized display name (e.g. "anglais" in French UI, "English" in English UI).
 * Falls back to the original code if it can't be resolved.
 */
private fun languageCodeToDisplayName(langCode: String, targetLocale: Locale = Locale.getDefault()): String {
    val deprecated = mapOf(
        "iw" to "he", // Hebrew old code
        "in" to "id", // Indonesian old code
        "ji" to "yi"  // Yiddish old code
    )
    val trimmed = langCode.trim()
    if (trimmed.isEmpty()) return trimmed

    // Normalize underscores to hyphens and map deprecated primary subtags
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
                    ))

                // Subtle bottom gradient to improve contrast with the duration badge
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

                // Duration badge (bottom-right), similar to YouTube style
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
