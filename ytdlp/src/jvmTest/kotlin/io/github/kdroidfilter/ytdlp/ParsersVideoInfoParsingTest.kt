package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.parseVideoInfoFromJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParsersVideoInfoParsingTest {
    @Test
    fun parseVideoInfoFromJson_extractsResolutionsAndDirectUrl() {
        // Minimal JSON sample approximating yt-dlp --dump-json output
        val json = """
            {
              "id": "abc123def45",
              "title": "Test Video",
              "webpage_url": "https://www.youtube.com/watch?v=abc123def45",
              "formats": [
                {"format_id":"18","height":360,"ext":"mp4","acodec":"aac","vcodec":"h264","protocol":"https","url":"https://cdn.example.com/v360.mp4"},
                {"format_id":"22","height":720,"ext":"mp4","acodec":"aac","vcodec":"h264","protocol":"https","url":"https://cdn.example.com/v720.mp4"},
                {"format_id":"137","height":1080,"ext":"mp4","acodec":"none","vcodec":"h264","protocol":"https","url":"https://cdn.example.com/v1080.mp4"},
                {"format_id":"95","height":136,"ext":"mp4","acodec":"aac","vcodec":"h264","protocol":"m3u8","url":"https://cdn.example.com/hls.m3u8"}
              ]
            }
        """.trimIndent()

        val info = parseVideoInfoFromJson(json, maxHeight = 1080, preferredExts = listOf("mp4", "webm"))

        // Basic fields
        assertEquals("abc123def45", info.id)
        assertEquals("Test Video", info.title)
        assertEquals("https://www.youtube.com/watch?v=abc123def45", info.url)

        // Resolutions: includes progressive at 360p and 720p, video-only at 1080p
        val res = info.availableResolutions
        assertTrue(res[360]?.downloadable == true)
        assertTrue(res[360]?.progressive == true)
        assertTrue(res[720]?.downloadable == true)
        assertTrue(res[720]?.progressive == true)
        assertTrue(res[1080]?.downloadable == true)
        assertTrue(res[1080]?.progressive == false) // video-only

        // Direct URL should pick the best progressive within maxHeight (720p)
        assertEquals("https://cdn.example.com/v720.mp4", info.directUrl)
        assertNotNull(info.directUrlFormat)
    }
}

