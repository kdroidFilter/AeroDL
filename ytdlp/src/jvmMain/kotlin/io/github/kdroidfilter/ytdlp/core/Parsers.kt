package io.github.kdroidfilter.ytdlp.core

import io.github.kdroidfilter.ytdlp.model.*
import io.github.kdroidfilter.ytdlp.util.warnln
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
    if (formats == null || formats.isEmpty()) return emptyMap()

    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val progressiveHeights = mutableSetOf<Int>()
    val videoOnlyHeights = mutableSetOf<Int>()

    formats.forEach { formatEl ->
        val format = formatEl.objOrNull() ?: return@forEach
        val height = format["height"].intOrNull() ?: return@forEach
        val vcodec = format["vcodec"].strOrNull()
        val acodec = format["acodec"].strOrNull()

        if (vcodec != null && vcodec != "none") {
            if (acodec != null && acodec != "none") {
                progressiveHeights.add(height)
            } else {
                videoOnlyHeights.add(height)
            }
        }
    }

    val allHeights = (progressiveHeights + videoOnlyHeights).toSet()
    return allHeights.associateWith { height ->
        ResolutionAvailability(
            progressive = height in progressiveHeights,
            downloadable = true // A resolution is downloadable if it exists as progressive or video-only
        )
    }
}


// --- JSON Parsers ---
fun parseVideoInfoFromJson(
    jsonString: String,
    maxHeight: Int = 1080,
    preferredExts: List<String> = listOf("mp4", "webm")
): VideoInfo {
    // Local helpers to avoid polluting the namespace
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull
    fun JsonElement?.longOrNull() = this?.jsonPrimitive?.longOrNull
    fun JsonElement?.doubleOrNull() = this?.jsonPrimitive?.doubleOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try {
        json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (e: Exception) {
        warnln { "JSON parsing for VideoInfo failed, returning empty object. Error: ${e.message}" }
        buildJsonObject { }
    }

    val id = root["id"].strOrNull() ?: root["url"].strOrNull() ?: ""
    val url = root["url"].strOrNull() ?: root["webpage_url"].strOrNull() ?: ""
    val title = root["title"].strOrNull() ?: "Unknown"
    val duration = root["duration"].doubleOrNull()?.let { java.time.Duration.ofSeconds(it.toLong()) }
    val uploader = root["uploader"].strOrNull() ?: root["channel"].strOrNull()
    val uploaderUrl = root["uploader_url"].strOrNull() ?: root["channel_url"].strOrNull()

    val availableSubtitles: Map<String, SubtitleInfo> = buildMap {
        root["subtitles"].objOrNull()?.forEach { (lang, dataEl) ->
            val arr = dataEl.arrOrNull() ?: return@forEach
            val formats = arr.mapNotNull { fe ->
                val fo = fe.objOrNull() ?: return@mapNotNull null
                val ext = fo["ext"].strOrNull() ?: return@mapNotNull null
                SubtitleFormat(ext = ext, url = fo["url"].strOrNull(), name = fo["name"].strOrNull())
            }
            if (formats.isNotEmpty()) {
                put(lang, SubtitleInfo(language = lang, languageName = arr.firstOrNull()?.objOrNull()?.get("name")?.strOrNull(), formats = formats))
            }
        }
    }

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
        thumbnail = root["thumbnail"].strOrNull(),
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
        chapters = chapters,
        tags = (root["tags"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList(),
        categories = (root["categories"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList(),
        directUrl = directUrl,
        directUrlFormat = directUrlFormat,
        availableResolutions = availableResolutions
    )
}

fun parsePlaylistInfoFromJson(jsonString: String): PlaylistInfo {
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try { json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (e: Exception) {
        warnln { "JSON parsing for PlaylistInfo failed, returning empty object. Error: ${e.message}" }
        buildJsonObject { }
    }

    val entries: List<VideoInfo> = root["entries"].arrOrNull()
        ?.mapNotNull { el ->
            try {
                parseVideoInfoFromJson(el.objOrNull().toString())
            } catch (e: Exception) {
                warnln { "Failed to parse a video entry within a playlist: ${e.message}" }
                null
            }
        } ?: emptyList()

    return PlaylistInfo(
        id = root["id"].strOrNull(),
        title = root["title"].strOrNull(),
        description = root["description"].strOrNull(),
        uploader = root["uploader"].strOrNull(),
        uploaderUrl = root["uploader_url"].strOrNull(),
        entries = entries,
        entryCount = root["playlist_count"].intOrNull() ?: entries.size
    )
}
