package io.github.kdroidfilter.ytdlpgui.features.download.single

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.design.components.EllipsizedTextWithTooltip
import io.github.kdroidfilter.ytdlpgui.core.design.components.Switcher
import io.github.kdroidfilter.ytdlpgui.core.design.components.TrimSlider
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppAccentButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppCardExpanderItem
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppColors
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppMenuContainer
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppMenuItem
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppMenuSeparator
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppProgressRing
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSegmentedButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSegmentedControl
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import ytdlpgui.composeapp.generated.resources.*
import java.time.Duration
import java.util.*

@Composable
fun SingleDownloadScreen(
    navController: androidx.navigation.NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry
) {
    val appGraph = LocalAppGraph.current
    val viewModel = remember(backStackEntry) {
        appGraph.singleDownloadViewModelFactory.create(backStackEntry.savedStateHandle)
    }
    val state by viewModel.uiState.collectAsState()

    // Handle navigation to Downloader directly via NavController
    LaunchedEffect(state.navigationState) {
        when (state.navigationState) {
            is SingleDownloadNavigationState.NavigateToDownloader -> {
                navController.navigate(io.github.kdroidfilter.ytdlpgui.core.navigation.Destination.MainNavigation.Downloader)
                viewModel.handleEvent(SingleDownloadEvents.OnNavigationConsumed)
            }

            SingleDownloadNavigationState.None -> {
                // no-op
            }
        }
    }

    SingleDownloadView(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

@Composable
fun SingleDownloadView(
    state: SingleDownloadState,
    onEvent: (SingleDownloadEvents) -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose {
            onEvent(SingleDownloadEvents.ScreenDisposed)
        }
    }
    val videoPlayerState = rememberVideoPlayerState()
    if (state.isLoading) Loader()
    else if (state.errorMessage != null) ErrorBox(state.errorMessage)
    else SingleVideoDownloadView(
        videoPlayerState = videoPlayerState,
        videoInfo = state.videoInfo,
        availablePresets = state.availablePresets,
        selectedPreset = state.selectedPreset,
        availableSubtitles = state.availableSubtitles,
        selectedSubtitles = state.selectedSubtitles,
        availableAudioQualityPresets = state.availableAudioQualityPresets,
        selectedAudioQualityPreset = state.selectedAudioQualityPreset,
        splitChapters = state.splitChapters,
        hasSponsorSegments = state.hasSponsorSegments,
        removeSponsors = state.removeSponsors,
        // Trim state
        trimStartMs = state.trimStartMs,
        trimEndMs = state.trimEndMs,
        totalDurationMs = state.totalDurationMs,
        showTrimSlider = state.showTrimSlider,
        isTrimmed = state.isTrimmed,
        onSelectPreset = { onEvent(SingleDownloadEvents.SelectPreset(it)) },
        onSelectAudioQualityPreset = { onEvent(SingleDownloadEvents.SelectAudioQualityPreset(it)) },
        onToggleSubtitle = { onEvent(SingleDownloadEvents.ToggleSubtitle(it)) },
        onClearSubtitles = { onEvent(SingleDownloadEvents.ClearSubtitles) },
        onSetSplitChapters = { onEvent(SingleDownloadEvents.SetSplitChapters(it)) },
        onSetRemoveSponsors = { onEvent(SingleDownloadEvents.SetRemoveSponsors(it)) },
        onSetTrimRange = { start, end -> onEvent(SingleDownloadEvents.SetTrimRange(start, end)) },
        onStartDownload = { onEvent(SingleDownloadEvents.StartDownload) },
        onStartAudioDownload = { onEvent(SingleDownloadEvents.StartAudioDownload) }
    )

}

@Composable
private fun Loader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppProgressRing()
    }
}

@Composable
private fun ErrorBox(message: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(
            imageVector = AppIcons.ErrorCircle,
            contentDescription = stringResource(Res.string.cd_error_icon),
            modifier = Modifier.size(144.dp),
            tint = AppColors.critical
        )
        Spacer(Modifier.size(16.dp))
        AppText(
            text = stringResource(Res.string.error_fetch_video_info, message),
            color = AppColors.critical,
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
    availableSubtitles: Map<String, io.github.kdroidfilter.ytdlp.model.SubtitleInfo>,
    selectedSubtitles: List<String>,
    availableAudioQualityPresets: List<YtDlpWrapper.AudioQualityPreset>,
    selectedAudioQualityPreset: YtDlpWrapper.AudioQualityPreset?,
    splitChapters: Boolean,
    hasSponsorSegments: Boolean,
    removeSponsors: Boolean,
    // Trim state
    trimStartMs: Long,
    trimEndMs: Long,
    totalDurationMs: Long,
    showTrimSlider: Boolean,
    isTrimmed: Boolean,
    onSelectPreset: (YtDlpWrapper.Preset) -> Unit,
    onSelectAudioQualityPreset: (YtDlpWrapper.AudioQualityPreset) -> Unit,
    onToggleSubtitle: (String) -> Unit,
    onClearSubtitles: () -> Unit,
    onSetSplitChapters: (Boolean) -> Unit,
    onSetRemoveSponsors: (Boolean) -> Unit,
    onSetTrimRange: (Long, Long) -> Unit,
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

            if (availablePresets.isNotEmpty() || availableAudioQualityPresets.isNotEmpty()) {
                // Afficher les options vidéo seulement si "Vidéo" est sélectionné
                if (selectedFormatIndex == 0 && availablePresets.isNotEmpty()) {
                    // Sélecteur de résolution avec SegmentedControl
                    AppText(text = stringResource(Res.string.single_formats))
                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availablePresets.forEachIndexed { index, preset ->
                            AppSegmentedControl(selectedIndex = if (preset == selectedPreset) 0 else -1) {
                                AppSegmentedButton(
                                    index = 0,
                                    selected = preset == selectedPreset,
                                    onClick = { onSelectPreset(preset) },
                                ) {
                                    AppText("${preset.height}p")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Sélecteur de sous-titres avec MenuFlyoutContainer
                    if (availableSubtitles.isNotEmpty()) {
                        var resetKey by remember { mutableStateOf(0) }
                        val sortedLanguages = remember(resetKey, availableSubtitles) {
                            availableSubtitles.keys.sortedBy {
                                languageCodeToDisplayName(it).lowercase(Locale.getDefault())
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppText(text = stringResource(Res.string.single_subtitles))

                            var menuExpanded by remember { mutableStateOf(false) }
                            val autoLabel = stringResource(Res.string.single_auto_generated)

                            AppMenuContainer(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                trigger = {
                                    AppButton(
                                        onClick = { menuExpanded = !menuExpanded },
                                    ) {
                                        AppText(
                                            if (selectedSubtitles.isEmpty())
                                                stringResource(Res.string.single_no_subtitle)
                                            else
                                                selectedSubtitles.joinToString(", ") { lang ->
                                                    val subtitleInfo = availableSubtitles[lang]
                                                    val displayName = languageCodeToDisplayName(lang)
                                                    val isAutoGenerated =
                                                        subtitleInfo?.autoGenerated == true || subtitleInfo?.isAutomatic == true
                                                    if (isAutoGenerated) "$displayName ($autoLabel)" else displayName
                                                }
                                        )
                                    }
                                },
                                content = {
                                    AppMenuItem(
                                        onClick = {
                                            onClearSubtitles()
                                            resetKey += 1
                                            menuExpanded = false
                                        },
                                        text = stringResource(Res.string.single_no_subtitle),
                                    )
                                    AppMenuSeparator()
                                    sortedLanguages.forEach { lang ->
                                        val isSelected = selectedSubtitles.contains(lang)
                                        val subtitleInfo = availableSubtitles[lang]
                                        val displayName = languageCodeToDisplayName(lang)
                                        val isAutoGenerated =
                                            subtitleInfo?.autoGenerated == true || subtitleInfo?.isAutomatic == true
                                        val fullDisplayName =
                                            if (isAutoGenerated) "$displayName ($autoLabel)" else displayName
                                        val displayText = if (isSelected) "\u2713 $fullDisplayName" else "    $fullDisplayName"
                                        AppMenuItem(
                                            onClick = {
                                                onToggleSubtitle(lang)
                                            },
                                            text = displayText,
                                        )
                                    }
                                },
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    // Option: Split chapters (video) — friendlier CardExpander with caption
                    if (videoInfo?.hasChapters == true) {
                        AppCardExpanderItem(
                            heading = {
                                AppText(
                                    stringResource(Res.string.split_chapters),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            caption = {
                                EllipsizedTextWithTooltip(
                                    text = stringResource(Res.string.single_split_chapters_caption),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            icon = { AppIcon(AppIcons.FilmstripPlay, null) },
                            trailing = {
                                Switcher(
                                    checked = splitChapters,
                                    onCheckStateChange = onSetSplitChapters,
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Option: SponsorBlock (video) — friendlier CardExpander with caption
                    if (hasSponsorSegments) {
                        AppCardExpanderItem(
                            heading = {
                                AppText(
                                    stringResource(Res.string.remove_sponsors),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            caption = {
                                EllipsizedTextWithTooltip(
                                    text = stringResource(Res.string.single_sponsorblock_caption),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            icon = { AppIcon(AppIcons.Info, null) },
                            trailing = {
                                Switcher(
                                    checked = removeSponsors,
                                    onCheckStateChange = onSetRemoveSponsors,
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Trim slider (video mode, when not using split-chapters)
                    if (showTrimSlider && !splitChapters) {
                        TrimSlider(
                            trimStartMs = trimStartMs,
                            trimEndMs = trimEndMs,
                            totalDurationMs = totalDurationMs,
                            isTrimmed = isTrimmed,
                            onTrimRangeChange = onSetTrimRange
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Afficher le sélecteur de qualité audio si "Audio" est sélectionné
                if (selectedFormatIndex == 1 && availableAudioQualityPresets.isNotEmpty()) {
                    // Sélecteur de qualité audio
                    AppText(text = stringResource(Res.string.single_audio_quality))
                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableAudioQualityPresets.forEachIndexed { index, audioPreset ->
                            AppSegmentedControl(selectedIndex = if (audioPreset == selectedAudioQualityPreset) 0 else -1) {
                                AppSegmentedButton(
                                    index = 0,
                                    selected = audioPreset == selectedAudioQualityPreset,
                                    onClick = { onSelectAudioQualityPreset(audioPreset) },
                                ) {
                                    AppText(audioPreset.bitrate)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Option: Split chapters (audio) — friendlier CardExpander with caption
                    if (videoInfo?.hasChapters == true) {
                        AppCardExpanderItem(
                            heading = {
                                AppText(
                                    stringResource(Res.string.split_chapters),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            caption = {
                                EllipsizedTextWithTooltip(
                                    text = stringResource(Res.string.single_split_chapters_caption),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            icon = { AppIcon(AppIcons.FilmstripPlay, null) },
                            trailing = {
                                Switcher(
                                    checked = splitChapters,
                                    onCheckStateChange = onSetSplitChapters,
                                )
                            }
                        )
                    }

                    // Option: SponsorBlock (audio) — friendlier CardExpander with caption
                    if (hasSponsorSegments) {
                        Spacer(Modifier.height(8.dp))
                        AppCardExpanderItem(
                            heading = {
                                AppText(
                                    stringResource(Res.string.remove_sponsors),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            caption = {
                                EllipsizedTextWithTooltip(
                                    text = stringResource(Res.string.single_sponsorblock_caption),
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                )
                            },
                            icon = { AppIcon(AppIcons.Info, null) },
                            trailing = {
                                Switcher(
                                    checked = removeSponsors,
                                    onCheckStateChange = onSetRemoveSponsors,
                                )
                            }
                        )
                    }

                    // Trim slider (audio mode, when not using split-chapters)
                    if (showTrimSlider && !splitChapters) {
                        Spacer(Modifier.height(16.dp))
                        TrimSlider(
                            trimStartMs = trimStartMs,
                            trimEndMs = trimEndMs,
                            totalDurationMs = totalDurationMs,
                            isTrimmed = isTrimmed,
                            onTrimRangeChange = onSetTrimRange
                        )
                    }
                }

                // Bouton de téléchargement centré
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppAccentButton(onClick = if (selectedFormatIndex == 0) onStartDownload else onStartAudioDownload) {
                        AppIcon(AppIcons.Download, null)
                        AppText(stringResource(Res.string.download_button))
                    }

                    // Split-chapters now controlled by a checkbox above
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
        videoLabel to AppIcons.Video,
        audioLabel to AppIcons.MusicNote
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                imageVector = AppIcons.FilmstripPlay,
                contentDescription = stringResource(Res.string.cd_filmstrip_play)
            )
            AppText(stringResource(Res.string.single_choose_format))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppSegmentedControl(selectedIndex = selectedFormatIndex) {
                options.forEachIndexed { index, (label, icon) ->
                    AppSegmentedButton(
                        index = index,
                        selected = index == selectedFormatIndex,
                        onClick = { onFormatSelected(index) },
                    ) {
                        AppIcon(icon, contentDescription = label)
                        Spacer(Modifier.width(4.dp))
                        AppText(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTitle(videoInfo: VideoInfo?) {
    AppText(
        text = videoInfo?.title.orEmpty(),
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VideoDescription(videoInfo: VideoInfo?) {
    if (videoInfo?.description?.isNotEmpty() == true) {
        var expanded by remember { mutableStateOf(false) }
        var hasOverflow by remember(videoInfo.description) { mutableStateOf(false) }
        val degrees by animateFloatAsState(if (expanded) -90f else 90f)

        Column {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        enabled = hasOverflow,
                        indication = null,
                        interactionSource = null
                    ) { expanded = !expanded }
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        imageVector = AppIcons.Textbox,
                        contentDescription = stringResource(Res.string.cd_textbox)
                    )
                    AppText(stringResource(Res.string.single_description), style = AppTypography.body)
                }
                if (hasOverflow) {
                    AppIcon(
                        AppIcons.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.rotate(degrees),
                    )
                }
            }

            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                // Using Material Text here because AppText does not support onTextLayout
                androidx.compose.material.Text(
                    text = videoInfo.description.orEmpty(),
                    style = AppTypography.body,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (!expanded && textLayoutResult.hasVisualOverflow) {
                            hasOverflow = true
                        }
                    }
                )
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
        AppText(text = text, fontSize = 12.sp, color = Color.White)
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
                AppIcon(
                    imageVector = AppIcons.Play,
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
                            AppProgressRing(modifier = Modifier.size(48.dp))
                        }
                        if (isHovered) {
                            if (!videoPlayerState.isPlaying) {
                                IconButton({ videoPlayerState.play() }) {
                                    AppIcon(
                                        imageVector = AppIcons.Play,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White
                                    )
                                }
                            } else {
                                IconButton({ videoPlayerState.pause() }) {
                                    AppIcon(
                                        imageVector = AppIcons.Pause,
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

@Preview
@Composable
fun SingleDownloadScreenPreviewLoading() {
    SingleDownloadView(state = SingleDownloadState.loadingState, onEvent = {})
}

@Preview
@Composable
fun SingleDownloadScreenPreviewEmpty() {
    SingleDownloadView(state = SingleDownloadState.emptyState, onEvent = {})
}

@Preview
@Composable
fun SingleDownloadScreenPreviewError() {
    SingleDownloadView(state = SingleDownloadState.errorState, onEvent = {})
}

@Preview
@Composable
fun SingleDownloadScreenPreviewLoaded() {
    SingleDownloadView(state = SingleDownloadState.loadedState, onEvent = {})
}

@Preview
@Composable
fun SingleDownloadScreenPreviewWithSubtitles() {
    SingleDownloadView(state = SingleDownloadState.withSubtitlesState, onEvent = {})
}
