package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeThumbnailHelperTest {

    @Test
    fun extractVideoId_fromVariousUrls() {
        assertEquals("dQw4w9WgXcQ", YouTubeThumbnailHelper.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeThumbnailHelper.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeThumbnailHelper.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeThumbnailHelper.extractVideoId("dQw4w9WgXcQ"))
        assertNull(YouTubeThumbnailHelper.extractVideoId("https://vimeo.com/123456"))
    }

    @Test
    fun isYouTubeUrl_detectsDomains() {
        assertTrue(YouTubeThumbnailHelper.isYouTubeUrl("https://music.youtube.com/watch?v=abc"))
        assertTrue(YouTubeThumbnailHelper.isYouTubeUrl("https://youtu.be/abc"))
        assertFalse(YouTubeThumbnailHelper.isYouTubeUrl("https://example.com"))
    }

    @Test
    fun getThumbnailUrl_buildsCorrectPath() {
        val url = YouTubeThumbnailHelper.getThumbnailUrl("dQw4w9WgXcQ")
        assertTrue(url.contains("/vi/dQw4w9WgXcQ/"))
        assertTrue(url.endsWith("hqdefault.jpg"))
    }

    @Test
    fun playlistId_helpers() {
        val pid = YouTubeThumbnailHelper.extractPlaylistId("https://www.youtube.com/playlist?list=PL12345ABC")
        assertEquals("PL12345ABC", pid)
        val thumb = YouTubeThumbnailHelper.getPlaylistThumbnailUrl("PL12345ABC")
        assertTrue(thumb.contains("PL12345ABC"))
    }
}

