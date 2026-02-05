package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import io.github.kdroidfilter.youtubewebviewextractor.YouTubeScrapedVideo
import io.github.kdroidfilter.youtubewebviewextractor.YouTubeWebViewExtractor
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppAccentButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppCheckBox
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppColors
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcon
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppIcons
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppProgressRing
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSegmentedButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSegmentedControl
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppSubtleButton
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppText
import io.github.kdroidfilter.ytdlpgui.core.design.themed.AppTypography
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.platform.browser.openUrlInBrowser
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
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
        playlistUrl = viewModel.playlistUrl,
        onEvent = viewModel::handleEvent,
        onLoginStatusChecked = viewModel::onLoginStatusChecked,
        onExtractionProgress = viewModel::onExtractionProgress,
        onFallbackExtractionComplete = { viewModel.handleEvent(BulkDownloadEvents.OnFallbackExtractionComplete) },
        onFallbackExtractionError = viewModel::onFallbackExtractionError
    )
}

@Composable
fun BulkDownloadView(
    state: BulkDownloadState,
    playlistUrl: String,
    onEvent: (BulkDownloadEvents) -> Unit,
    onLoginStatusChecked: (Boolean?) -> Unit,
    onExtractionProgress: (Int) -> Unit,
    onFallbackExtractionComplete: () -> Unit,
    onFallbackExtractionError: (String) -> Unit
) {
    DisposableEffect(Unit) {
        onDispose {
            onEvent(BulkDownloadEvents.ScreenDisposed)
        }
    }

    when {
        state.isLoading -> Loader()
        state.fallbackState != FallbackState.None && state.fallbackState != FallbackState.Completed -> {
            FallbackContent(
                state = state,
                playlistUrl = playlistUrl,
                onEvent = onEvent,
                onLoginStatusChecked = onLoginStatusChecked,
                onExtractionProgress = onExtractionProgress,
                onFallbackExtractionComplete = onFallbackExtractionComplete,
                onFallbackExtractionError = onFallbackExtractionError
            )
        }
        state.errorMessage != null -> ErrorBox(state.errorMessage)
        state.videos.isEmpty() -> EmptyPlaylist()
        else -> PlaylistContent(state, onEvent)
    }
}

@Composable
private fun Loader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppProgressRing()
            Spacer(Modifier.height(16.dp))
            AppText(stringResource(Res.string.loading))
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
        AppIcon(
            imageVector = AppIcons.VideoClipMultiple,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = AppColors.textSecondary
        )
        Spacer(Modifier.height(16.dp))
        AppText(
            text = stringResource(Res.string.bulk_no_videos),
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaylistContent(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit
) {
    val appGraph = LocalAppGraph.current
    val imageLoader = appGraph.imageLoader
    val videoListState = rememberLazyListState()
    val optionsScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with stats and select all
        PlaylistHeader(state, onEvent)
        Spacer(Modifier.height(8.dp))

        // Scrollable video list
        Row(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = videoListState,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                items(
                    items = state.videos,
                    key = { it.videoInfo.id }
                ) { videoItem ->
                    VideoRow(
                        item = videoItem,
                        imageLoader = imageLoader,
                        onToggle = { onEvent(BulkDownloadEvents.ToggleVideoSelection(videoItem.videoInfo.id)) }
                    )
                    Divider(
                        color = AppColors.controlSecondary,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(videoListState),
                modifier = Modifier.fillMaxHeight().padding(top = 2.dp, start = 8.dp)
            )
        }

        // Download options zone (fixed height, scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(optionsScrollState)
                    .padding(vertical = 8.dp)
            ) {
                DownloadOptions(state, onEvent)
                Spacer(Modifier.height(16.dp))
                DownloadButton(state, onEvent)
                Spacer(Modifier.height(8.dp))
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(optionsScrollState),
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PlaylistHeader(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = stringResource(Res.string.bulk_videos_count, state.totalCount),
                style = AppTypography.caption
            )

            if (state.isCheckingAvailability) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppProgressRing(modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    AppText(
                        text = "${state.checkedCount}/${state.totalCount}",
                        style = AppTypography.caption
                    )
                }
            }

            AppText(
                text = stringResource(Res.string.bulk_selected_count, state.selectedCount),
                style = AppTypography.caption,
                color = AppColors.fillAccentDefault,
                fontWeight = FontWeight.Medium
            )
        }

        // Toggle select all / deselect all
        val allSelected = state.allSelected
        AppSubtleButton(
            onClick = {
                if (allSelected) onEvent(BulkDownloadEvents.DeselectAll)
                else onEvent(BulkDownloadEvents.SelectAll)
            }
        ) {
            AppIcon(
                if (allSelected) AppIcons.SelectAllOff else AppIcons.SelectAllOn,
                if (allSelected) stringResource(Res.string.bulk_deselect_all)
                else stringResource(Res.string.bulk_select_all)
            )
        }
    }
}

@Composable
private fun VideoRow(
    item: BulkVideoItem,
    imageLoader: ImageLoader,
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
                AppProgressRing(modifier = Modifier.size(20.dp))
            } else {
                AppCheckBox(
                    checked = item.isSelected,
                    onCheckedChange = { if (isEnabled) onToggle() }
                )
            }
        }

        // Thumbnail (like DownloadScreen)
        VideoThumbnail(item, imageLoader)

        Spacer(Modifier.width(8.dp))

        // Video info
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AppText(
                text = item.videoInfo.title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.videoInfo.uploader?.let { uploader ->
                    AppText(
                        text = uploader,
                        style = AppTypography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.videoInfo.duration?.let { duration ->
                    AppText(
                        text = formatDuration(duration),
                        style = AppTypography.caption
                    )
                }
                if (!item.isAvailable) {
                    AppText(
                        text = stringResource(Res.string.bulk_video_unavailable),
                        style = AppTypography.caption,
                        color = AppColors.critical,
                        modifier = Modifier.clickable { openUrlInBrowser(item.videoInfo.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(item: BulkVideoItem, imageLoader: ImageLoader) {
    Column(
        modifier = Modifier.width(88.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val alpha = if (item.isAvailable) 1f else 0.5f

            AsyncImage(
                model = item.videoInfo.thumbnail,
                contentDescription = stringResource(Res.string.thumbnail_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = alpha,
                imageLoader = imageLoader
            )

            if (!item.isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        imageVector = AppIcons.ErrorCircle,
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
                AppIcon(AppIcons.FilmstripPlay, contentDescription = null)
                AppText(stringResource(Res.string.single_choose_format))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppSegmentedControl(selectedIndex = if (state.isAudioMode) 1 else 0) {
                    AppSegmentedButton(
                        index = 0,
                        selected = !state.isAudioMode,
                        onClick = { onEvent(BulkDownloadEvents.SetAudioMode(false)) },
                    ) {
                        AppIcon(AppIcons.Video, contentDescription = videoLabel)
                        Spacer(Modifier.width(4.dp))
                        AppText(videoLabel)
                    }
                    AppSegmentedButton(
                        index = 1,
                        selected = state.isAudioMode,
                        onClick = { onEvent(BulkDownloadEvents.SetAudioMode(true)) },
                    ) {
                        AppIcon(AppIcons.MusicNote, contentDescription = audioLabel)
                        Spacer(Modifier.width(4.dp))
                        AppText(audioLabel)
                    }
                }
            }
        }

        // Quality selector (like SingleDownloadScreen)
        if (!state.isAudioMode && state.availablePresets.isNotEmpty()) {
            AppText(text = stringResource(Res.string.single_formats))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availablePresets.forEach { preset ->
                    AppSegmentedControl(selectedIndex = if (preset == state.selectedPreset) 0 else -1) {
                        AppSegmentedButton(
                            index = 0,
                            selected = preset == state.selectedPreset,
                            onClick = { onEvent(BulkDownloadEvents.SelectPreset(preset)) },
                        ) {
                            AppText("${preset.height}p")
                        }
                    }
                }
            }
        }

        if (state.isAudioMode && state.availableAudioQualityPresets.isNotEmpty()) {
            AppText(text = stringResource(Res.string.single_audio_quality))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableAudioQualityPresets.forEach { preset ->
                    AppSegmentedControl(selectedIndex = if (preset == state.selectedAudioQualityPreset) 0 else -1) {
                        AppSegmentedButton(
                            index = 0,
                            selected = preset == state.selectedAudioQualityPreset,
                            onClick = { onEvent(BulkDownloadEvents.SelectAudioQualityPreset(preset)) },
                        ) {
                            AppText(preset.bitrate)
                        }
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
        AppAccentButton(
            onClick = { if (canStart) onEvent(BulkDownloadEvents.StartDownloads) }
        ) {
            if (state.isStartingDownloads) {
                AppProgressRing(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                AppText(stringResource(Res.string.bulk_starting_downloads))
            } else {
                AppIcon(AppIcons.Download, null)
                Spacer(Modifier.width(8.dp))
                AppText(stringResource(Res.string.bulk_download_selected) + " (${state.selectedCount})")
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

@Composable
private fun FallbackContent(
    state: BulkDownloadState,
    playlistUrl: String,
    onEvent: (BulkDownloadEvents) -> Unit,
    onLoginStatusChecked: (Boolean?) -> Unit,
    onExtractionProgress: (Int) -> Unit,
    onFallbackExtractionComplete: () -> Unit,
    onFallbackExtractionError: (String) -> Unit
) {
    infoln { "[FallbackContent] FallbackState: ${state.fallbackState}" }
    val extractor = state.webViewExtractor
    if (extractor == null) {
        infoln { "[FallbackContent] WebViewExtractor is null, returning" }
        return
    }

    when (state.fallbackState) {
        is FallbackState.LoginRequired -> {
            // Show login WebView
            YouTubeLoginScreen(
                extractor = extractor,
                onBack = { onEvent(BulkDownloadEvents.CancelFallback) },
                onLoginSuccess = { onEvent(BulkDownloadEvents.OnUserLoggedIn) }
            )
        }
        is FallbackState.CheckingLogin, is FallbackState.Extracting -> {
            // Show loader with progress
            ExtractionProgress(state.fallbackState)

            // Hidden WebView in separate invisible window
            HiddenExtractionWebView(
                url = playlistUrl,
                extractor = extractor,
                fallbackState = state.fallbackState,
                onLoginStatusChecked = onLoginStatusChecked,
                onExtractionProgress = onExtractionProgress,
                onExtractionComplete = onFallbackExtractionComplete,
                onExtractionError = onFallbackExtractionError
            )
        }
        is FallbackState.Error -> {
            ErrorBox((state.fallbackState as FallbackState.Error).message)
        }
        else -> { }
    }
}

@Composable
private fun ExtractionProgress(fallbackState: FallbackState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppProgressRing()
            Spacer(Modifier.height(16.dp))
            AppText(stringResource(Res.string.loading))
            if (fallbackState is FallbackState.Extracting && fallbackState.videoCount > 0) {
                Spacer(Modifier.height(8.dp))
                AppText(
                    text = "(${stringResource(Res.string.bulk_analyzing_videos, fallbackState.videoCount)})",
                    style = AppTypography.caption,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun YouTubeLoginScreen(
    extractor: YouTubeWebViewExtractor,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val state = rememberWebViewState("https://www.youtube.com/account_advanced")
    val navigator = rememberWebViewNavigator()

    var isCheckingLogin by remember { mutableStateOf(false) }

    // Check login status when page loads
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.lastLoadedUrl?.contains("youtube.com") == true) {
            isCheckingLogin = true
            extractor.checkLoginStatus(navigator) { loggedIn ->
                isCheckingLogin = false
                if (loggedIn == true) {
                    onLoginSuccess()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    imageVector = AppIcons.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    AppText(
                        text = stringResource(Res.string.bulk_login_required_title),
                        style = AppTypography.bodyStrong
                    )
                    AppText(
                        text = stringResource(Res.string.bulk_login_required_desc),
                        style = AppTypography.caption,
                        color = AppColors.textSecondary
                    )
                }
            }

            // Login status indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isCheckingLogin -> {
                        AppProgressRing(modifier = Modifier.size(16.dp))
                        AppText(
                            text = stringResource(Res.string.bulk_login_check),
                            style = AppTypography.caption
                        )
                    }
                    extractor.isLoggedIn == true -> {
                        AppIcon(
                            imageVector = AppIcons.Check,
                            contentDescription = null,
                            tint = AppColors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        AppText(
                            text = stringResource(Res.string.bulk_login_success),
                            style = AppTypography.caption,
                            color = AppColors.success
                        )
                        AppAccentButton(onClick = onLoginSuccess) {
                            AppText(stringResource(Res.string.bulk_login_continue))
                        }
                    }
                }
            }
        }

        // WebView
        WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private val json = Json { ignoreUnknownKeys = true }

private val checkLoginStatusJs = """
    (function() {
        var avatarBtn = document.querySelector('#avatar-btn');
        if (avatarBtn) return 'true';
        var signInBtn = document.querySelector('a[href*="accounts.google.com/ServiceLogin"]');
        if (signInBtn) return 'false';
        return 'unknown';
    })();
""".trimIndent()

private val extractPlaylistLinksJs = """
    (function() {
        var items = [...document.querySelectorAll('ytd-playlist-video-renderer')].map(renderer => {
            var titleEl = renderer.querySelector('a#video-title');
            if (!titleEl) return null;
            var url = new URL(titleEl.href);
            var videoId = url.searchParams.get('v');
            if (!videoId) return null;
            var durationEl = renderer.querySelector('span#text.ytd-thumbnail-overlay-time-status-renderer');
            var duration = durationEl ? durationEl.textContent.trim() : null;
            return {
                url: 'https://www.youtube.com/watch?v=' + videoId,
                title: titleEl.textContent.trim(),
                duration: duration,
                thumbnail: 'https://i.ytimg.com/vi/' + videoId + '/mqdefault.jpg'
            };
        }).filter(item => item !== null);
        return JSON.stringify(items);
    })();
""".trimIndent()

private val scrollAndCountJs = """
    (function() {
        window.scrollTo(0, document.documentElement.scrollHeight);
        var playlistItems = document.querySelectorAll('a#video-title, a#video-title-link');
        return playlistItems.length.toString();
    })();
""".trimIndent()

@Composable
private fun HiddenExtractionWebView(
    url: String,
    extractor: YouTubeWebViewExtractor,
    fallbackState: FallbackState,
    onLoginStatusChecked: (Boolean?) -> Unit,
    onExtractionProgress: (Int) -> Unit,
    onExtractionComplete: () -> Unit,
    onExtractionError: (String) -> Unit
) {
    val isChannelUrl = remember(url) { extractor.isChannelUrl(url) }
    val initialUrl = remember(url) { extractor.normalizeUrl(url) }
    infoln { "[HiddenExtractionWebView] Initial URL: $initialUrl, isChannel: $isChannelUrl" }

    val state = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()

    var loginChecked by remember { mutableStateOf(false) }
    var channelConverted by remember { mutableStateOf(false) }
    var playlistUrl by remember { mutableStateOf<String?>(null) }
    var isScrolling by remember { mutableStateOf(false) }
    var lastVideoCount by remember { mutableStateOf(0) }
    var noChangeCount by remember { mutableStateOf(0) }

    // Check login status when page loads
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && !loginChecked && state.lastLoadedUrl?.contains("youtube.com") == true) {
            infoln { "[HiddenExtractionWebView] Page loaded, checking login status..." }
            loginChecked = true
            navigator.evaluateJavaScript(checkLoginStatusJs) { result ->
                val status = result?.removeSurrounding("\"")?.trim()
                val loggedIn = when (status) {
                    "true" -> true
                    "false" -> false
                    else -> null
                }
                infoln { "[HiddenExtractionWebView] Login status: $loggedIn" }
                onLoginStatusChecked(loggedIn)
            }
        }
    }

    // Convert channel to playlist when in Extracting state
    LaunchedEffect(fallbackState, state.isLoading, channelConverted) {
        if (fallbackState is FallbackState.Extracting && !state.isLoading && isChannelUrl && !channelConverted) {
            infoln { "[HiddenExtractionWebView] Channel detected, extracting channel ID..." }
            delay(1000) // Wait for page to fully load

            extractor.extractChannelId(navigator) { channelId ->
                if (channelId != null) {
                    val uploadsPlaylistUrl = extractor.channelIdToUploadsPlaylistUrl(channelId)
                    infoln { "[HiddenExtractionWebView] Converted to playlist URL: $uploadsPlaylistUrl" }
                    playlistUrl = uploadsPlaylistUrl
                    channelConverted = true
                    navigator.loadUrl(uploadsPlaylistUrl)
                } else {
                    infoln { "[HiddenExtractionWebView] Failed to extract channel ID" }
                    onExtractionError("Failed to extract channel ID")
                }
            }
        }
    }

    // Start scrolling when in Extracting state and on playlist page
    LaunchedEffect(fallbackState, state.isLoading, channelConverted) {
        if (fallbackState is FallbackState.Extracting && !state.isLoading && !isScrolling) {
            val currentUrl = state.lastLoadedUrl ?: return@LaunchedEffect

            // For channels, wait until we've navigated to the playlist
            if (isChannelUrl && !channelConverted) return@LaunchedEffect

            // Only start scrolling on playlist pages
            if (currentUrl.contains("/playlist")) {
                delay(1500) // Wait for content to load
                infoln { "[HiddenExtractionWebView] Starting auto-scroll on playlist..." }
                isScrolling = true
                lastVideoCount = 0
                noChangeCount = 0
            }
        }
    }

    // Auto-scroll loop
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            while (isScrolling) {
                navigator.evaluateJavaScript(scrollAndCountJs) { result ->
                    val count = result?.removeSurrounding("\"")?.toIntOrNull() ?: 0
                    infoln { "[HiddenExtractionWebView] Scrolling... $count videos loaded" }
                    onExtractionProgress(count)

                    if (count == lastVideoCount) {
                        noChangeCount++
                    } else {
                        noChangeCount = 0
                        lastVideoCount = count
                    }

                    // If no change after 3 scrolls, stop and extract
                    if (noChangeCount >= 3) {
                        infoln { "[HiddenExtractionWebView] Extracting $count videos..." }
                        isScrolling = false

                        navigator.evaluateJavaScript(extractPlaylistLinksJs) { extractResult ->
                            if (extractResult == null) {
                                onExtractionError("Null result from extraction")
                                return@evaluateJavaScript
                            }
                            try {
                                val jsonString = json.decodeFromString<String>(extractResult)
                                val videos = json.decodeFromString<List<YouTubeScrapedVideo>>(jsonString)
                                    .distinctBy { it.url }
                                infoln { "[HiddenExtractionWebView] Extracted ${videos.size} videos" }
                                extractor.updateExtractedVideos(videos)
                                onExtractionComplete()
                            } catch (e: Exception) {
                                infoln { "[HiddenExtractionWebView] Parsing error: ${e.message}" }
                                onExtractionError(e.message ?: "Unknown parsing error")
                            }
                        }
                    }
                }

                delay(1000)
            }
        }
    }

    // Visible window for WebView (needed for proper rendering)
   Window(
        onCloseRequest = { },
        visible = true,
        title = "WebView Extractor",
        state = WindowState(
            width = 800.dp,
            height = 600.dp,
            position = WindowPosition.Aligned(Alignment.Center)),
        undecorated = true,
        alwaysOnTop = false,
        resizable = false,
        focusable = false,

    ) {
       LaunchedEffect(Unit) {
           window.opacity = 0f
       }
        WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier.fillMaxSize()
        )
    }
}
