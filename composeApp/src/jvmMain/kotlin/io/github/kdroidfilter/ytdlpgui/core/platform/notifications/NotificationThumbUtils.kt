package io.github.kdroidfilter.ytdlpgui.core.platform.notifications

import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper

object NotificationThumbUtils {
    /**
     * Resolve a thumbnail URL to use in notifications. Prefers a provided direct thumbnail,
     * otherwise tries to derive a YouTube thumbnail from the URL if possible.
     */
    fun resolveThumbnailUrl(directThumb: String?, url: String): String? {
        if (!directThumb.isNullOrBlank()) return directThumb
        val id = YouTubeThumbnailHelper.extractVideoId(url) ?: return null
        return YouTubeThumbnailHelper.getThumbnailUrl(id, YouTubeThumbnailHelper.ThumbnailQuality.MEDIUM)
    }
}
