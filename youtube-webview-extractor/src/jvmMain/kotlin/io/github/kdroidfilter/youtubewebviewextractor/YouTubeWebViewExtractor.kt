package io.github.kdroidfilter.youtubewebviewextractor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.webview.web.WebViewNavigator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Service for extracting YouTube playlist/channel videos via WebView scxxxxxx.
 * Used as fallback when yt-dlp fails to fetch playlist info.
 */
class YouTubeWebViewExtractor {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Current login status: true = logged in, false = not logged in, null = unknown/checking
     */
    var isLoggedIn: Boolean? by mutableStateOf(null)
        private set

    /**
     * Current extraction state
     */
    var extractionState: ExtractionState by mutableStateOf(ExtractionState.Idle)
        private set

    /**
     * Extracted videos (available after successful extraction)
     */
    var extractedVideos: List<YouTubeScrapedVideo> by mutableStateOf(emptyList())
        private set

    /**
     * Updates the extracted videos (called from external extraction logic).
     */
    fun updateExtractedVideos(videos: List<YouTubeScrapedVideo>) {
        extractedVideos = videos
        extractionState = ExtractionState.Completed(videos.size)
    }

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
     * JavaScript to extract video links from a YouTube playlist.
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
     * JavaScript to extract video links from a YouTube channel (/videos page).
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
     * JavaScript to scroll and count videos.
     */
    private val scrollAndCountJs = """
        (function() {
            window.scrollTo(0, document.documentElement.scrollHeight);
            var playlistItems = document.querySelectorAll('a#video-title, a#video-title-link');
            return playlistItems.length.toString();
        })();
    """.trimIndent()

    /**
     * JavaScript to extract channel ID from the page.
     * Looks for the channel ID in various places in the YouTube page.
     */
    private val extractChannelIdJs = """
        (function() {
            // Try to get from canonical URL
            var canonical = document.querySelector('link[rel="canonical"]');
            if (canonical) {
                var href = canonical.getAttribute('href');
                var match = href.match(/\/channel\/(UC[a-zA-Z0-9_-]+)/);
                if (match) return match[1];
            }
            // Try to get from meta tag
            var meta = document.querySelector('meta[itemprop="channelId"]');
            if (meta) return meta.getAttribute('content');
            // Try to get from ytInitialData
            if (typeof ytInitialData !== 'undefined' && ytInitialData.metadata) {
                var channelId = ytInitialData.metadata.channelMetadataRenderer?.externalId;
                if (channelId) return channelId;
            }
            // Try from page source
            var scripts = document.querySelectorAll('script');
            for (var script of scripts) {
                var text = script.textContent;
                var match = text.match(/"channelId":"(UC[a-zA-Z0-9_-]+)"/);
                if (match) return match[1];
            }
            return null;
        })();
    """.trimIndent()

    /**
     * Checks the Google login status via the WebView.
     */
    fun checkLoginStatus(navigator: WebViewNavigator, onResult: (Boolean?) -> Unit) {
        navigator.evaluateJavaScript(checkLoginStatusJs) { result ->
            val status = result?.removeSurrounding("\"")?.trim()
            isLoggedIn = when (status) {
                "true" -> true
                "false" -> false
                else -> null
            }
            infoln { "[YouTubeWebViewExtractor] Login status: $isLoggedIn" }
            onResult(isLoggedIn)
        }
    }

    /**
     * Starts the extraction process for the given URL.
     * The WebView should already be loaded with the target URL.
     */
    suspend fun startExtraction(
        navigator: WebViewNavigator,
        currentUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Result<List<YouTubeScrapedVideo>> {
        extractionState = ExtractionState.Scrolling(0)
        extractedVideos = emptyList()

        val isPlaylist = currentUrl.contains("/playlist")
        val extractJs = if (isPlaylist) extractPlaylistLinksJs else extractChannelLinksJs

        var lastVideoCount = 0
        var noChangeCount = 0
        val maxNoChangeCount = 3

        // Auto-scroll loop
        while (noChangeCount < maxNoChangeCount) {
            val countDeferred = CompletableDeferred<Int>()

            navigator.evaluateJavaScript(scrollAndCountJs) { result ->
                val count = result?.removeSurrounding("\"")?.toIntOrNull() ?: 0
                countDeferred.complete(count)
            }

            val count = countDeferred.await()
            extractionState = ExtractionState.Scrolling(count)
            onProgress(count)

            infoln { "[YouTubeWebViewExtractor] Scrolling... $count videos loaded" }

            if (count == lastVideoCount) {
                noChangeCount++
            } else {
                noChangeCount = 0
                lastVideoCount = count
            }

            delay(1000)
        }

        // Extract links
        extractionState = ExtractionState.Extracting
        infoln { "[YouTubeWebViewExtractor] Extracting $lastVideoCount videos..." }

        val resultDeferred = CompletableDeferred<Result<List<YouTubeScrapedVideo>>>()

        navigator.evaluateJavaScript(extractJs) { result ->
            if (result == null) {
                resultDeferred.complete(Result.failure(Exception("Null result from JavaScript")))
                return@evaluateJavaScript
            }

            try {
                val jsonString = json.decodeFromString<String>(result)
                val videos = json.decodeFromString<List<YouTubeScrapedVideo>>(jsonString)
                    .distinctBy { it.url }

                extractedVideos = videos
                extractionState = ExtractionState.Completed(videos.size)
                infoln { "[YouTubeWebViewExtractor] Extracted ${videos.size} videos" }
                resultDeferred.complete(Result.success(videos))
            } catch (e: Exception) {
                infoln { "[YouTubeWebViewExtractor] Parsing error: ${e.message}" }
                extractionState = ExtractionState.Error(e.message ?: "Unknown error")
                resultDeferred.complete(Result.failure(e))
            }
        }

        return resultDeferred.await()
    }

    /**
     * Resets the extractor state.
     */
    fun reset() {
        extractionState = ExtractionState.Idle
        extractedVideos = emptyList()
        isLoggedIn = null
    }

    /**
     * Normalizes a YouTube URL.
     * - Converts watch URLs with list param to playlist URLs
     * - For playlists, returns as-is.
     * - For channels, returns the channel URL (will be converted to playlist later).
     */
    fun normalizeUrl(url: String): String {
        // If it's a watch URL with a playlist ID, convert to playlist URL
        if (url.contains("/watch") && url.contains("list=")) {
            val listId = extractPlaylistIdFromUrl(url)
            if (listId != null) {
                return "https://www.youtube.com/playlist?list=$listId"
            }
        }

        // If it's already a playlist, return as-is
        if (url.contains("/playlist")) return url

        // For channels, just return the base channel URL (we'll extract ID and convert to playlist)
        return when {
            url.contains("/@") -> url.substringBefore("/videos").substringBefore("/streams").substringBefore("/shorts")
            url.contains("/channel/") -> url.substringBefore("/videos").substringBefore("/streams").substringBefore("/shorts")
            else -> url
        }
    }

    /**
     * Extracts playlist ID from a URL containing list= parameter.
     */
    private fun extractPlaylistIdFromUrl(url: String): String? {
        val regex = Regex("[?&]list=([a-zA-Z0-9_-]+)")
        return regex.find(url)?.groupValues?.get(1)
    }

    /**
     * Checks if the URL is a channel URL (not a playlist).
     */
    fun isChannelUrl(url: String): Boolean {
        return !url.contains("/playlist") && (url.contains("/@") || url.contains("/channel/"))
    }

    /**
     * Checks if the URL is a playlist URL.
     */
    fun isPlaylistUrl(url: String): Boolean {
        return url.contains("/playlist")
    }

    /**
     * Extracts the channel ID from the current page via JavaScript.
     */
    fun extractChannelId(navigator: WebViewNavigator, onResult: (String?) -> Unit) {
        navigator.evaluateJavaScript(extractChannelIdJs) { result ->
            val channelId = result?.removeSurrounding("\"")?.trim()?.takeIf {
                it != "null" && it.startsWith("UC")
            }
            infoln { "[YouTubeWebViewExtractor] Extracted channel ID: $channelId" }
            onResult(channelId)
        }
    }

    /**
     * Converts a channel ID (UC...) to the uploads playlist URL (UU...).
     */
    fun channelIdToUploadsPlaylistUrl(channelId: String): String {
        // Replace 'UC' prefix with 'UU' to get the uploads playlist ID
        val playlistId = "UU" + channelId.removePrefix("UC")
        return "https://www.youtube.com/playlist?list=$playlistId"
    }

    /**
     * Checks if the URL is a valid YouTube playlist or channel URL.
     */
    fun isValidYouTubeUrl(url: String): Boolean {
        return url.contains("/playlist") ||
                url.contains("/videos") ||
                url.contains("/@") ||
                url.contains("/channel")
    }

    sealed class ExtractionState {
        data object Idle : ExtractionState()
        data class Scrolling(val videoCount: Int) : ExtractionState()
        data object Extracting : ExtractionState()
        data class Completed(val videoCount: Int) : ExtractionState()
        data class Error(val message: String) : ExtractionState()
    }
}
