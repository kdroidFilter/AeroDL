package io.github.kdroidfilter.youtubeplaylistextractor

data class YouTubePlaylistVideo(
    val url: String,
    val title: String,
    val duration: String? = null,
    val thumbnail: String? = null,
) {
    val videoId: String?
        get() = url.substringAfter("v=", "").substringBefore("&").takeIf { it.isNotEmpty() }
}

data class YouTubePlaylist(
    val title: String,
    val videos: List<YouTubePlaylistVideo>,
)
