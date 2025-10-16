package io.github.kdroidfilter.ytdlp.util

/**
 * Helper object to generate YouTube thumbnail URLs from video IDs
 */
object YouTubeThumbnailHelper {
    
    /**
     * YouTube thumbnail quality options
     */
    enum class ThumbnailQuality(val suffix: String, val description: String) {
        MAX_RES("maxresdefault", "Maximum resolution (1920x1080 if available)"),
        HIGH("hqdefault", "High quality (480x360)"),
        MEDIUM("mqdefault", "Medium quality (320x180)"),
        STANDARD("sddefault", "Standard quality (640x480)"),
        DEFAULT("default", "Default quality (120x90)")
    }
    
    /**
     * Generates a YouTube thumbnail URL from a video ID
     * @param videoId The YouTube video ID (e.g., "dQw4w9WgXcQ")
     * @param quality The desired thumbnail quality
     * @return The thumbnail URL
     */
    fun getThumbnailUrl(videoId: String, quality: ThumbnailQuality = ThumbnailQuality.HIGH): String {
        return "https://img.youtube.com/vi/$videoId/${quality.suffix}.jpg"
    }
    
    /**
     * Generates multiple thumbnail URLs for fallback purposes
     * @param videoId The YouTube video ID
     * @return A list of thumbnail URLs ordered from highest to lowest quality
     */
    fun getThumbnailUrls(videoId: String): List<String> {
        return listOf(
            ThumbnailQuality.MAX_RES,
            ThumbnailQuality.STANDARD,
            ThumbnailQuality.HIGH,
            ThumbnailQuality.MEDIUM,
            ThumbnailQuality.DEFAULT
        ).map { quality ->
            getThumbnailUrl(videoId, quality)
        }
    }
    
    /**
     * Extracts YouTube video ID from various URL formats
     * @param url The YouTube URL
     * @return The video ID if found, null otherwise
     */
    fun extractVideoId(url: String): String? {
        // Handle different YouTube URL formats
        val patterns = listOf(
            // Standard watch URL: youtube.com/watch?v=VIDEO_ID
            Regex("""[?&]v=([a-zA-Z0-9_-]{11})"""),
            // Short URL: youtu.be/VIDEO_ID
            Regex("""youtu\.be/([a-zA-Z0-9_-]{11})"""),
            // Embed URL: youtube.com/embed/VIDEO_ID
            Regex("""youtube\.com/embed/([a-zA-Z0-9_-]{11})"""),
            // YouTube Music: music.youtube.com/watch?v=VIDEO_ID
            Regex("""music\.youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        // If no pattern matched but the string is exactly 11 characters (likely just the ID)
        if (url.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))) {
            return url
        }
        
        return null
    }
    
    /**
     * Checks if a URL is a YouTube URL
     * @param url The URL to check
     * @return true if it's a YouTube URL
     */
    fun isYouTubeUrl(url: String): Boolean {
        val youtubePatterns = listOf(
            "youtube.com",
            "youtu.be",
            "music.youtube.com",
            "m.youtube.com",
            "www.youtube.com"
        )
        return youtubePatterns.any { domain -> url.contains(domain) }
    }
    
    /**
     * Extracts playlist ID from YouTube playlist URL
     * @param url The YouTube playlist URL
     * @return The playlist ID if found, null otherwise
     */
    fun extractPlaylistId(url: String): String? {
        val pattern = Regex("""[?&]list=([a-zA-Z0-9_-]+)""")
        val match = pattern.find(url)
        return match?.groupValues?.get(1)
    }
    
    /**
     * For playlists, the thumbnail is typically the thumbnail of the first video
     * This method generates a placeholder URL that YouTube will redirect
     * @param playlistId The YouTube playlist ID
     * @return A URL that can be used as a playlist thumbnail
     */
    fun getPlaylistThumbnailUrl(playlistId: String): String {
        // YouTube playlists don't have direct thumbnail URLs,
        // but we can use the YouTube API endpoint or the first video's thumbnail
        // This returns a URL format that some services recognize
        return "https://i.ytimg.com/vi/$playlistId/hqdefault.jpg"
    }
}