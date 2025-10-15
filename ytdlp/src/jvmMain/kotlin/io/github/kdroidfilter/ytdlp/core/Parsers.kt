package io.github.kdroidfilter.ytdlp.core

import io.github.kdroidfilter.ytdlp.model.*
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.infoln
import kotlinx.serialization.json.*

// --- Helper to find the best direct URL from a formats array ---
private fun findBestDirectUrl(
    formats: JsonArray?,
    maxHeight: Int,
    preferredExts: List<String>
): Pair<String?, String?> {
    if (formats == null || formats.isEmpty()) return null to null

    // Helper to extract format properties safely
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    // First, try to find a progressive format (contains both video and audio)
    val progressiveFormats = formats.mapNotNull { formatEl ->
        val format = formatEl.objOrNull() ?: return@mapNotNull null
        val url = format["url"].strOrNull() ?: return@mapNotNull null
        val height = format["height"].intOrNull()
        val ext = format["ext"].strOrNull()
        val acodec = format["acodec"].strOrNull()
        val vcodec = format["vcodec"].strOrNull()
        val protocol = format["protocol"].strOrNull()

        // Exclude m3u8/HLS streams as they are not direct download links
        if (protocol == "m3u8" || protocol == "m3u8_native") {
            return@mapNotNull null
        }

        // Check if it's a progressive format (has both audio and video)
        if (acodec != null && acodec != "none" &&
            vcodec != null && vcodec != "none" &&
            height != null && height <= maxHeight) {
            Triple(url, height, ext)
        } else null
    }

    // Sort by height (descending), then by preferred extension
    val sorted = progressiveFormats.sortedWith(
        compareByDescending<Triple<String, Int, String?>> { it.second }
            .thenBy { triple ->
                val ext = triple.third
                if (ext != null && ext in preferredExts) {
                    preferredExts.indexOf(ext)
                } else {
                    preferredExts.size
                }
            }
    )

    return sorted.firstOrNull()?.let { (url, height, ext) ->
        url to "progressive_${height}p${if (ext != null) ".$ext" else ""}"
    } ?: (null to null)
}

// --- Helper to parse resolution availability from a formats array ---
private fun parseResolutionAvailability(formats: JsonArray?): Map<Int, ResolutionAvailability> {
    debugln { "[parseResolutionAvailability] Starting to parse formats" }
    debugln { "[parseResolutionAvailability] formats == null? ${formats == null}" }
    debugln { "[parseResolutionAvailability] formats.isEmpty()? ${formats?.isEmpty()}" }
    debugln { "[parseResolutionAvailability] formats.size = ${formats?.size}" }

    if (formats == null || formats.isEmpty()) {
        debugln { "[parseResolutionAvailability] Returning empty map - no formats" }
        return emptyMap()
    }

    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val progressiveHeights = mutableSetOf<Int>()
    val videoOnlyHeights = mutableSetOf<Int>()

    var formatIndex = 0
    formats.forEach { formatEl ->
        formatIndex++
        val format = formatEl.objOrNull()
        if (format == null) {
            debugln { "[parseResolutionAvailability] Format #$formatIndex: Not a JsonObject, skipping" }
            return@forEach
        }

        val formatId = format["format_id"].strOrNull()
        val height = format["height"].intOrNull()
        val vcodec = format["vcodec"].strOrNull()
        val acodec = format["acodec"].strOrNull()
        val protocol = format["protocol"].strOrNull()

        debugln { "[parseResolutionAvailability] Format #$formatIndex (id=$formatId): height=$height, vcodec=$vcodec, acodec=$acodec, protocol=$protocol" }

        if (height == null) {
            debugln { "[parseResolutionAvailability]   -> Skipping: no height" }
            return@forEach
        }

        if (vcodec != null && vcodec != "none") {
            if (acodec != null && acodec != "none") {
                progressiveHeights.add(height)
                debugln { "[parseResolutionAvailability]   -> Added to PROGRESSIVE: ${height}p" }
            } else {
                videoOnlyHeights.add(height)
                debugln { "[parseResolutionAvailability]   -> Added to VIDEO-ONLY: ${height}p" }
            }
        } else {
            debugln { "[parseResolutionAvailability]   -> Skipping: vcodec is null or 'none'" }
        }
    }

    val allHeights = (progressiveHeights + videoOnlyHeights).toSet()
    infoln { "[parseResolutionAvailability] Summary: Progressive=${progressiveHeights.sorted()}, Video-only=${videoOnlyHeights.sorted()}, All=${allHeights.sorted()}" }

    return allHeights.associateWith { height ->
        ResolutionAvailability(
            progressive = height in progressiveHeights,
            downloadable = true // A resolution is downloadable if it exists as progressive or video-only
        )
    }
}

// --- Helper to parse subtitles from JSON ---
private fun parseSubtitles(subtitlesElement: JsonElement?, isAutomatic: Boolean = false): Map<String, SubtitleInfo> {
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull

    return buildMap {
        subtitlesElement?.objOrNull()?.forEach { (lang, dataEl) ->
            val arr = dataEl.arrOrNull() ?: return@forEach
            val formats = arr.mapNotNull { fe ->
                val fo = fe.objOrNull() ?: return@mapNotNull null
                val ext = fo["ext"].strOrNull() ?: return@mapNotNull null
                SubtitleFormat(
                    ext = ext,
                    url = fo["url"].strOrNull(),
                    name = fo["name"].strOrNull()
                )
            }
            if (formats.isNotEmpty()) {
                put(lang, SubtitleInfo(
                    language = lang,
                    languageName = arr.firstOrNull()?.objOrNull()?.get("name")?.strOrNull(),
                    formats = formats,
                    isAutomatic = isAutomatic
                ))
            }
        }
    }
}

// --- JSON Parsers ---
/**
 * Enhanced JSON parser for VideoInfo that auto-generates YouTube thumbnails when missing
 * and includes automatic captions
 */
fun parseVideoInfoFromJson(
    jsonString: String,
    maxHeight: Int = 1080,
    preferredExts: List<String> = listOf("mp4", "webm")
): VideoInfo {
    // Local helpers
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull
    fun JsonElement?.longOrNull() = this?.jsonPrimitive?.longOrNull
    fun JsonElement?.doubleOrNull() = this?.jsonPrimitive?.doubleOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try {
        json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (_: Exception) { buildJsonObject { } }

    val id = root["id"].strOrNull() ?: root["url"].strOrNull() ?: ""
    val url = root["url"].strOrNull() ?: root["webpage_url"].strOrNull() ?: ""
    val title = root["title"].strOrNull() ?: "Unknown"

    // Try to get thumbnail from JSON
    var thumbnail = root["thumbnail"].strOrNull()

    // If no thumbnail and it's a YouTube video, generate it from the ID
    if (thumbnail == null && url.isNotBlank()) {
        if (YouTubeThumbnailHelper.isYouTubeUrl(url)) {
            // First try to extract from URL
            val videoId = YouTubeThumbnailHelper.extractVideoId(url) ?: id
            if (videoId.isNotBlank() && videoId.length == 11) {
                // Generate high quality thumbnail URL
                thumbnail = YouTubeThumbnailHelper.getThumbnailUrl(
                    videoId,
                    YouTubeThumbnailHelper.ThumbnailQuality.HIGH
                )
            }
        }
    }

    val duration = root["duration"].doubleOrNull()?.let { java.time.Duration.ofSeconds(it.toLong()) }
    val uploader = root["uploader"].strOrNull() ?: root["channel"].strOrNull()
    val uploaderUrl = root["uploader_url"].strOrNull() ?: root["channel_url"].strOrNull()

    // Parse both manual and automatic subtitles
    val manualSubtitles = parseSubtitles(root["subtitles"], isAutomatic = false)
    val automaticSubtitles = parseSubtitles(root["automatic_captions"], isAutomatic = true)

    // Merge subtitles, preferring manual over automatic for same language
    val availableSubtitles = buildMap {
        putAll(automaticSubtitles) // Add automatic first
        putAll(manualSubtitles)     // Manual will override if same language exists
    }

    // Also create a separate map for automatic captions for backward compatibility
    val automaticCaptions = automaticSubtitles

    val chapters: List<ChapterInfo> = root["chapters"].arrOrNull()?.mapNotNull { el ->
        val o = el.objOrNull() ?: return@mapNotNull null
        val start = o["start_time"].doubleOrNull() ?: return@mapNotNull null
        val end = o["end_time"].doubleOrNull() ?: return@mapNotNull null
        ChapterInfo(title = o["title"].strOrNull(), startTime = start, endTime = end)
    } ?: emptyList()

    val formats = root["formats"].arrOrNull()

    // Extract direct URL from the formats array
    val (directUrl, directUrlFormat) = findBestDirectUrl(formats, maxHeight, preferredExts)

    // Extract available resolutions from the same formats array
    val availableResolutions = parseResolutionAvailability(formats)

    return VideoInfo(
        id = id,
        title = title,
        url = url,
        thumbnail = thumbnail,
        duration = duration,
        description = root["description"].strOrNull(),
        uploader = uploader,
        uploaderUrl = uploaderUrl,
        uploadDate = root["upload_date"].strOrNull(),
        viewCount = root["view_count"].longOrNull(),
        likeCount = root["like_count"].longOrNull(),
        width = root["width"].intOrNull(),
        height = root["height"].intOrNull(),
        fps = root["fps"].doubleOrNull(),
        formatNote = root["format_note"].strOrNull(),
        availableSubtitles = availableSubtitles,
        automaticCaptions = automaticCaptions,
        hasChapters = chapters.isNotEmpty(),
        chapters = chapters,
        tags = (root["tags"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList(),
        categories = (root["categories"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList(),
        directUrl = directUrl,
        directUrlFormat = directUrlFormat,
        availableResolutions = availableResolutions
    )
}

/**
 * Enhanced JSON parser for PlaylistInfo with thumbnail support
 */
fun parsePlaylistInfoFromJson(jsonString: String): PlaylistInfo {
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try {
        json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (_: Exception) {
        buildJsonObject { }
    }

    val playlistId = root["id"].strOrNull()
    val playlistUrl = root["webpage_url"].strOrNull() ?: root["url"].strOrNull()

    // Parse entries and ensure they have thumbnails
    val entries: List<VideoInfo> = root["entries"].arrOrNull()
        ?.mapNotNull { el ->
            try {
                // When parsing entries, the enhanced parseVideoInfoFromJson will auto-generate thumbnails
                parseVideoInfoFromJson(el.objOrNull().toString())
            } catch (_: Exception) {
                null
            }
        } ?: emptyList()

    // Try to get playlist thumbnail
    var playlistThumbnail = root["thumbnail"].strOrNull()

    // If no playlist thumbnail, try to use the first video's thumbnail
    if (playlistThumbnail == null && entries.isNotEmpty()) {
        playlistThumbnail = entries.first().thumbnail
    }

    // If still no thumbnail and it's a YouTube playlist, try to generate one
    if (playlistThumbnail == null && playlistUrl != null && YouTubeThumbnailHelper.isYouTubeUrl(playlistUrl)) {
        val extractedPlaylistId = YouTubeThumbnailHelper.extractPlaylistId(playlistUrl) ?: playlistId
        if (extractedPlaylistId != null) {
            playlistThumbnail = YouTubeThumbnailHelper.getPlaylistThumbnailUrl(extractedPlaylistId)
        }
    }

    return PlaylistInfo(
        id = playlistId,
        title = root["title"].strOrNull(),
        description = root["description"].strOrNull(),
        uploader = root["uploader"].strOrNull(),
        uploaderUrl = root["uploader_url"].strOrNull(),
        thumbnail = playlistThumbnail,
        entries = entries,
        entryCount = root["playlist_count"].intOrNull() ?: entries.size
    )
}
