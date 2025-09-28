package io.github.kdroidfilter.ytdlp.core

import io.github.kdroidfilter.ytdlp.model.*
import kotlinx.serialization.json.*


// --- Helper to find best format from formats array ---
private fun findBestDirectUrl(
    formats: JsonArray?,
    maxHeight: Int,
    preferredExts: List<String>
): Pair<String?, String?> {
    if (formats == null || formats.isEmpty()) return null to null

    // Helper to extract format properties
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    // First, try to find a progressive format (has both video and audio)
    val progressiveFormats = formats.mapNotNull { formatEl ->
        val format = formatEl.objOrNull() ?: return@mapNotNull null
        val url = format["url"].strOrNull() ?: return@mapNotNull null
        val height = format["height"].intOrNull()
        val ext = format["ext"].strOrNull()
        val acodec = format["acodec"].strOrNull()
        val vcodec = format["vcodec"].strOrNull()
        val protocol = format["protocol"].strOrNull()

        // Exclude m3u8/HLS streams (IMPORTANT: this is the fix for the m3u8 issue)
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

// --- JSON Parsers (updated with directUrl extraction) ---
fun parseVideoInfoFromJson(
    jsonString: String,
    maxHeight: Int = 1080,
    preferredExts: List<String> = listOf("mp4", "webm")
): VideoInfo {
    // Helpers kept local to avoid leaking symbols:
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

    // Extract direct URL from formats (NEW)
    val formats = root["formats"].arrOrNull()
    val (directUrl, directUrlFormat) = findBestDirectUrl(formats, maxHeight, preferredExts)

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
        directUrl = directUrl,  // NEW
        directUrlFormat = directUrlFormat  // NEW
    )
}

fun parsePlaylistInfoFromJson(jsonString: String): PlaylistInfo {
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try { json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (_: Exception) { buildJsonObject { } }

    val entries: List<VideoInfo> = root["entries"].arrOrNull()
        ?.mapNotNull { el ->
            try { parseVideoInfoFromJson(el.objOrNull().toString()) } catch (_: Exception) { null }
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