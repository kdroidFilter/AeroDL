package io.github.kdroidfilter.youtubeplaylistextractor

import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.network.KtorConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Extracts YouTube playlist entries from `ytInitialData` over HTTP,
 * using Firefox cookies when available.
 *
 * YouTube currently serves two playlist item shapes:
 * classic `playlistVideoRenderer`, and the newer `lockupViewModel`.
 */
object YouTubePlaylistExtractor {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0"
    private const val DEFAULT_CLIENT_VERSION = "2.20250319.01.00"
    private const val MAX_CONTINUATION_PAGES = 100
    private const val YOUTUBE_VIDEO_ID_LENGTH = 11

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extract(
        url: String,
        onProgress: (Int) -> Unit = {},
    ): Result<YouTubePlaylist> {
        val httpClient = KtorConfig.createHttpClient()
        return try {
            extract(httpClient, url, onProgress)
        } catch (e: Exception) {
            errorln(e) { "[YouTubePlaylistExtractor] ${e.message}" }
            Result.failure(e)
        } finally {
            httpClient.close()
        }
    }

    internal suspend fun extract(
        httpClient: HttpClient,
        url: String,
        onProgress: (Int) -> Unit = {},
    ): Result<YouTubePlaylist> {
        val cookies = FirefoxCookies.loadForDomain("youtube.com")
        val cookieHeader = FirefoxCookies.toHeader(cookies)
        infoln { "[YouTubePlaylistExtractor] Loaded ${cookies.size} Firefox cookies" }

        val normalized = normalizeUrl(url)
        val playlistUrl = if (isChannelUrl(normalized)) {
            resolveUploadsPlaylistUrl(httpClient, normalized, cookieHeader)
                ?: return Result.failure(IllegalStateException("Failed to resolve channel uploads playlist"))
        } else {
            normalized
        }

        infoln { "[YouTubePlaylistExtractor] Fetching $playlistUrl" }
        val html = getHtml(httpClient, playlistUrl, cookieHeader)
        val initialData = extractYtInitialData(html)
            ?: return Result.failure(IllegalStateException("Could not find ytInitialData in page"))

        val root = json.parseToJsonElement(initialData)
        val title = playlistTitle(root)
        val videos = mutableListOf<YouTubePlaylistVideo>()
        videos += extractVideos(root)
        onProgress(videos.size)

        val clientVersion = innertubeClientVersion(html)
        val apiKey = innertubeApiKey(html)
        var continuation = findContinuationToken(root)
        var pages = 0
        while (continuation != null && pages < MAX_CONTINUATION_PAGES) {
            pages++
            val usedToken = continuation
            val pageRoot = fetchContinuation(
                httpClient = httpClient,
                continuation = usedToken,
                cookieHeader = cookieHeader,
                referer = playlistUrl,
                clientVersion = clientVersion,
                apiKey = apiKey,
            ) ?: break
            videos += extractVideos(pageRoot)
            onProgress(videos.size)
            continuation = findContinuationToken(pageRoot, excluding = usedToken)
        }

        val unique = videos.distinctBy { it.url }
        infoln { "[YouTubePlaylistExtractor] Extracted ${unique.size} videos from '$title'" }
        if (unique.isEmpty()) {
            return Result.failure(IllegalStateException("No videos found in playlist"))
        }
        return Result.success(YouTubePlaylist(title = title, videos = unique))
    }

    fun normalizeUrl(url: String): String {
        if (url.contains("/watch") && url.contains("list=")) {
            val listId = extractPlaylistIdFromUrl(url)
            if (listId != null) return "https://www.youtube.com/playlist?list=$listId"
        }
        if (url.contains("/playlist")) return url
        return when {
            url.contains("/@") -> url.substringBefore("/videos").substringBefore("/streams").substringBefore("/shorts")
            url.contains("/channel/") -> url.substringBefore("/videos").substringBefore("/streams").substringBefore("/shorts")
            else -> url
        }
    }

    fun isChannelUrl(url: String): Boolean =
        !url.contains("/playlist") && (
            url.contains("/@") ||
                url.contains("/channel/") ||
                url.contains("/c/") ||
                url.contains("/user/")
            )

    fun isPlaylistUrl(url: String): Boolean = url.contains("/playlist") || url.contains("list=")

    fun channelIdToUploadsPlaylistUrl(channelId: String): String {
        val playlistId = "UU" + channelId.removePrefix("UC")
        return "https://www.youtube.com/playlist?list=$playlistId"
    }

    internal fun extractYtInitialData(html: String): String? {
        val marker = "ytInitialData"
        val idx = html.indexOf(marker)
        if (idx < 0) return null
        val start = html.indexOf('{', idx)
        if (start < 0) return null
        return extractJsonObject(html, start)
    }

    internal fun parsePlaylistVideos(items: JsonArray): Pair<List<YouTubePlaylistVideo>, String?> {
        val videos = extractVideos(items)
        val continuation = findContinuationToken(items)
        return videos to continuation
    }

    internal fun extractVideos(root: JsonElement): List<YouTubePlaylistVideo> {
        val classic = findByKey(root, "playlistVideoRenderer").mapNotNull { parseClassicVideo(it) }
        if (classic.isNotEmpty()) return classic
        return findByKey(root, "lockupViewModel").mapNotNull { parseLockupVideo(it) }
    }

    private fun extractPlaylistIdFromUrl(url: String): String? {
        val regex = Regex("[?&]list=([a-zA-Z0-9_-]+)")
        return regex.find(url)?.groupValues?.get(1)
    }

    private suspend fun resolveUploadsPlaylistUrl(
        httpClient: HttpClient,
        channelUrl: String,
        cookieHeader: String,
    ): String? {
        val html = getHtml(httpClient, channelUrl, cookieHeader)
        val initialData = extractYtInitialData(html) ?: return null
        val root = json.parseToJsonElement(initialData)
        val channelId = root.jsonObjectOrNull()
            ?.obj("metadata")
            ?.obj("channelMetadataRenderer")
            ?.string("externalId")
            ?: findChannelId(html)
        if (channelId == null) {
            errorln { "[YouTubePlaylistExtractor] Channel ID not found" }
            return null
        }
        infoln { "[YouTubePlaylistExtractor] Channel ID $channelId -> uploads playlist" }
        return channelIdToUploadsPlaylistUrl(channelId)
    }

    private fun findChannelId(html: String): String? {
        val match = Regex("\"channelId\":\"(UC[a-zA-Z0-9_-]+)\"").find(html)
        return match?.groupValues?.get(1)
    }

    private suspend fun getHtml(httpClient: HttpClient, url: String, cookieHeader: String): String {
        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            if (cookieHeader.isNotBlank()) header(HttpHeaders.Cookie, cookieHeader)
        }
        val body = response.bodyAsText()
        checkYouTubeResponse(response.status.value, body, cookiesWereSent = cookieHeader.isNotBlank())
        return body
    }

    private suspend fun fetchContinuation(
        httpClient: HttpClient,
        continuation: String,
        cookieHeader: String,
        referer: String,
        clientVersion: String,
        apiKey: String?,
    ): JsonElement? {
        val endpoint = buildString {
            append("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
            if (!apiKey.isNullOrBlank()) append("&key=").append(apiKey)
        }
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB")
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("continuation", continuation)
        }
        val response = httpClient.post(endpoint) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.5")
            header("X-YouTube-Client-Name", "1")
            header("X-YouTube-Client-Version", clientVersion)
            header(HttpHeaders.Origin, "https://www.youtube.com")
            header(HttpHeaders.Referrer, referer)
            if (cookieHeader.isNotBlank()) header(HttpHeaders.Cookie, cookieHeader)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        checkYouTubeResponse(response.status.value, text, cookiesWereSent = cookieHeader.isNotBlank())
        return runCatching { json.parseToJsonElement(text) }.getOrNull()
    }

    private fun checkYouTubeResponse(status: Int, body: String, cookiesWereSent: Boolean) {
        val blocked = status == 418 ||
            "blockByNetFree" in body ||
            "netfree.link/block" in body ||
            "netfree.link/img/block-favicon" in body
        if (blocked) {
            val cookieHint = if (cookiesWereSent) "Logged-in Firefox cookies were sent." else "No Firefox cookies were found."
            error("NetFree blocked this request (HTTP $status). $cookieHint")
        }
        if (status !in 200..299) {
            error("YouTube returned HTTP $status: ${body.take(200)}")
        }
    }

    private fun playlistTitle(root: JsonElement): String =
        root.jsonObjectOrNull()
            ?.obj("metadata")
            ?.obj("playlistMetadataRenderer")
            ?.string("title")
            ?: "Playlist"

    private fun parseClassicVideo(video: JsonObject): YouTubePlaylistVideo? {
        val videoId = video.string("videoId") ?: return null
        val title = video.obj("title")
            ?.arr("runs")
            ?.firstOrNull()
            ?.jsonObjectOrNull()
            ?.string("text")
            ?: video.obj("title")?.string("simpleText")
            ?: return null
        val duration = video.obj("lengthText")?.string("simpleText")
        return YouTubePlaylistVideo(
            url = "https://www.youtube.com/watch?v=$videoId",
            title = title,
            duration = duration,
            thumbnail = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
        )
    }

    private fun parseLockupVideo(lockup: JsonObject): YouTubePlaylistVideo? {
        val contentId = lockup.string("contentId") ?: return null
        if (contentId.length != YOUTUBE_VIDEO_ID_LENGTH) return null
        val title = lockup.obj("metadata")
            ?.obj("lockupMetadataViewModel")
            ?.obj("title")
            ?.string("content")
            ?: return null
        val duration = findByKey(lockup, "thumbnailBadgeViewModel")
            .mapNotNull { it.string("text") }
            .firstOrNull { ':' in it }
        return YouTubePlaylistVideo(
            url = "https://www.youtube.com/watch?v=$contentId",
            title = title,
            duration = duration,
            thumbnail = "https://i.ytimg.com/vi/$contentId/mqdefault.jpg",
        )
    }

    private fun findContinuationToken(root: JsonElement, excluding: String? = null): String? =
        findByKey(root, "continuationCommand")
            .mapNotNull { it.string("token") }
            .firstOrNull { token -> token.isNotBlank() && token != excluding }

    private fun findByKey(root: JsonElement, key: String): List<JsonObject> {
        val found = mutableListOf<JsonObject>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el[key]?.jsonObjectOrNull()?.let { found += it }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(root)
        return found
    }

    private fun innertubeClientVersion(html: String): String =
        Regex("\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            ?: DEFAULT_CLIENT_VERSION

    private fun innertubeApiKey(html: String): String? =
        Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)

    private fun extractJsonObject(source: String, start: Int): String? {
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until source.length) {
            val c = source[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return source.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key]?.jsonObjectOrNull()
    private fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject
}
