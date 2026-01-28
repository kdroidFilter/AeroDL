package io.github.kdroidfilter.youtubewebviewextractor

import kotlinx.serialization.Serializable

/**
 * Represents a video scraped from YouTube via WebView.
 */
@Serializable
data class YouTubeScrapedVideo(
    val url: String,
    val title: String,
    val duration: String? = null,
    val thumbnail: String? = null
) {
    /**
     * Extracts the video ID from the URL.
     */
    val videoId: String?
        get() = url.substringAfter("v=", "").substringBefore("&").takeIf { it.isNotEmpty() }
}
