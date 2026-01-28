import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.compose.AsyncImage
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.WebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents a video link with its title, duration and thumbnail.
 */
@Serializable
data class VideoLink(
    val url: String,
    val title: String,
    val duration: String? = null,
    val thumbnail: String? = null
)

/**
 * JavaScript to detect Google login status via YouTube DOM.
 */
private val checkLoginStatusJs = """
    (function() {
        var avatarBtn = document.querySelector('#avatar-btn');
        if (avatarBtn) return 'true';

        var signInBtn = document.querySelector('a[href*="accounts.google.com/ServiceLogin"]');
        if (signInBtn) return 'false';

        return 'unknown';
    })();
""".trimIndent()

/**
 * JavaScript to extract video links from a YouTube playlist with titles, durations and thumbnails.
 */
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

/**
 * JavaScript to extract video links from a YouTube channel (/videos page) with titles, durations and thumbnails.
 */
private val extractChannelLinksJs = """
    (function() {
        var items = [...document.querySelectorAll('ytd-rich-item-renderer')].map(renderer => {
            var titleEl = renderer.querySelector('a#video-title-link');
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

/**
 * JavaScript to scroll to the bottom of the page and return the video element count.
 */
private val scrollAndCountJs = """
    (function() {
        window.scrollTo(0, document.documentElement.scrollHeight);
        var playlistItems = document.querySelectorAll('a#video-title, a#video-title-link');
        return playlistItems.length.toString();
    })();
""".trimIndent()

/**
 * Checks if the user is logged into Google via the WebView.
 */
fun checkGoogleLoginStatus(navigator: WebViewNavigator, onResult: (Boolean?) -> Unit) {
    navigator.evaluateJavaScript(checkLoginStatusJs) { result ->
        val status = result?.removeSurrounding("\"")?.trim()
        when (status) {
            "true" -> onResult(true)
            "false" -> onResult(false)
            else -> onResult(null)
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "YouTube Link Scraper"
    ) {
        MaterialTheme {
            WebViewDemo()
        }
    }
}

@Composable
fun WebViewDemo(){
    val webviewState = rememberWebViewState("https://www.youtube.com/@%D7%94%D7%A8%D7%9E%D7%91%D7%9D%D7%94%D7%99%D7%95%D7%9E%D7%99-%D7%935%D7%9B/videos")
    WebView(webviewState, modifier = Modifier.fillMaxSize())
}

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun YouTubeScraper() {
    var urlInput by remember { mutableStateOf("") }
    var extractedLinks by remember { mutableStateOf<List<VideoLink>>(emptyList()) }
    var status by remember { mutableStateOf("Enter a YouTube playlist or channel URL") }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Initial URL to check login status
    val initialUrl = "https://www.youtube.com"
    val state = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()

    // Check login status on initial load
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.lastLoadedUrl?.contains("youtube.com") == true) {
            checkGoogleLoginStatus(navigator) { loggedIn ->
                isLoggedIn = loggedIn
            }
        }
    }

    var lastVideoCount by remember { mutableStateOf(0) }
    var noChangeCount by remember { mutableStateOf(0) }
    var isScrolling by remember { mutableStateOf(false) }

    // Function to extract links
    fun extractLinks(extractJs: String) {
        navigator.evaluateJavaScript(extractJs) { result ->
            println("[WebViewDemo] Raw result: $result")
            if (result == null) {
                println("[WebViewDemo] Null result")
                isLoading = false
                isScrolling = false
                return@evaluateJavaScript
            }
            try {
                // The result is a JSON-encoded string, decode it first
                val jsonString = json.decodeFromString<String>(result)
                println("[WebViewDemo] JSON string: $jsonString")

                val links = json.decodeFromString<List<VideoLink>>(jsonString)
                    .distinctBy { it.url }
                println("[WebViewDemo] Parsed ${links.size} links")
                extractedLinks = links
                status = if (links.isNotEmpty()) {
                    "${links.size} videos found"
                } else {
                    "No videos found"
                }
            } catch (e: Exception) {
                println("[WebViewDemo] Parsing error: ${e.message}")
                e.printStackTrace()
                status = "Parsing error: ${e.message}"
            }
            isLoading = false
            isScrolling = false
            lastVideoCount = 0
            noChangeCount = 0
        }
    }

    // Start auto-scroll when page is loaded
    LaunchedEffect(state.isLoading, state.lastLoadedUrl) {
        if (!state.isLoading && isLoading && !isScrolling) {
            val currentUrl = state.lastLoadedUrl ?: return@LaunchedEffect
            kotlinx.coroutines.delay(1500)

            val isValidUrl = currentUrl.contains("/playlist") ||
                    currentUrl.contains("/videos") ||
                    currentUrl.contains("/@") ||
                    currentUrl.contains("/channel")

            if (isValidUrl) {
                isScrolling = true
                lastVideoCount = 0
                noChangeCount = 0
                status = "Auto-scrolling..."
            } else {
                status = "Unrecognized URL. Use a YouTube playlist or channel."
                isLoading = false
            }
        }
    }

    // Auto-scroll loop
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            val currentUrl = state.lastLoadedUrl ?: return@LaunchedEffect
            val extractJs = when {
                currentUrl.contains("/playlist") -> extractPlaylistLinksJs
                else -> extractChannelLinksJs
            }

            while (isScrolling) {
                // Scroll and count videos
                navigator.evaluateJavaScript(scrollAndCountJs) { result ->
                    val count = result?.removeSurrounding("\"")?.toIntOrNull() ?: 0
                    status = "Scrolling... $count videos loaded"

                    if (count == lastVideoCount) {
                        noChangeCount++
                    } else {
                        noChangeCount = 0
                        lastVideoCount = count
                    }

                    // If no change after 3 scrolls, stop and extract
                    if (noChangeCount >= 3) {
                        status = "Extracting $count videos..."
                        extractLinks(extractJs)
                    }
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Login status indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val connectionText = when (isLoggedIn) {
                true -> "Logged in to Google"
                false -> "Not logged in"
                null -> "Checking..."
            }
            val connectionColor = when (isLoggedIn) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.outline
            }
            Text(
                text = connectionText,
                color = connectionColor,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // URL input field
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("YouTube URL (playlist or channel)") },
            placeholder = { Text("https://www.youtube.com/playlist?list=... or https://www.youtube.com/@...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Scraping button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        // Normalize URL for channels
                        val targetUrl = when {
                            urlInput.contains("/@") && !urlInput.contains("/videos") ->
                                urlInput.trimEnd('/') + "/videos"
                            urlInput.contains("/channel/") && !urlInput.contains("/videos") ->
                                urlInput.trimEnd('/') + "/videos"
                            else -> urlInput
                        }
                        extractedLinks = emptyList()
                        status = "Loading page..."
                        isLoading = true
                        navigator.loadUrl(targetUrl)
                    }
                },
                enabled = urlInput.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Extract links")
            }

            if (extractedLinks.isNotEmpty()) {
                OutlinedButton(
                    onClick = { extractedLinks = emptyList() }
                ) {
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(8.dp))

        // Extracted links list
        if (extractedLinks.isNotEmpty()) {
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp)
                ) {
                    itemsIndexed(extractedLinks) { index, videoLink ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Number
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp)
                            )
                            // Thumbnail
                            videoLink.thumbnail?.let { thumbnailUrl ->
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = videoLink.title,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            // Title, duration and URL
                            SelectionContainer {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = videoLink.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        videoLink.duration?.let { duration ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = duration,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = videoLink.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (index < extractedLinks.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Invisible but functional WebView (1x1 pixel size, off-screen)
        Box(
            modifier = Modifier
                .size(1.dp)
                .absoluteOffset(x = (-9999).dp, y = (-9999).dp)
        ) {
            WebView(
                state = state,
                navigator = navigator,
                modifier = Modifier.requiredSize(800.dp, 600.dp)
            )
        }
    }
}
