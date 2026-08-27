package io.github.kdroidfilter.youtubeplaylistextractor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YouTubePlaylistExtractorTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun extractYtInitialData_readsJsonObjectFromHtml() {
        val html = """
            <html><script>
            var ytInitialData = {"metadata":{"playlistMetadataRenderer":{"title":"Demo"}}};
            </script></html>
        """.trimIndent()

        val extracted = YouTubePlaylistExtractor.extractYtInitialData(html)
        assertNotNull(extracted)
        val root = json.parseToJsonElement(extracted).jsonObject
        assertEquals(
            "Demo",
            root["metadata"]?.jsonObject
                ?.get("playlistMetadataRenderer")?.jsonObject
                ?.get("title")?.toString()?.trim('"')
        )
    }

    @Test
    fun parsePlaylistVideos_readsTitleDurationAndContinuation() {
        val payload = """
            [
              {
                "playlistVideoRenderer": {
                  "videoId": "abc123",
                  "title": {"runs": [{"text": "First video"}]},
                  "lengthText": {"simpleText": "12:34"}
                }
              },
              {
                "playlistVideoRenderer": {
                  "videoId": "def456",
                  "title": {"runs": [{"text": "Second video"}]}
                }
              },
              {
                "continuationItemRenderer": {
                  "continuationEndpoint": {
                    "continuationCommand": {"token": "CONT_TOKEN"}
                  }
                }
              }
            ]
        """.trimIndent()
        val items = json.parseToJsonElement(payload).jsonArray
        val (videos, continuation) = YouTubePlaylistExtractor.parsePlaylistVideos(items)

        assertEquals(2, videos.size)
        assertEquals("https://www.youtube.com/watch?v=abc123", videos[0].url)
        assertEquals("First video", videos[0].title)
        assertEquals("12:34", videos[0].duration)
        assertEquals("abc123", videos[0].videoId)
        assertEquals("https://i.ytimg.com/vi/abc123/mqdefault.jpg", videos[0].thumbnail)
        assertEquals("Second video", videos[1].title)
        assertEquals("CONT_TOKEN", continuation)
    }

    @Test
    fun parsePlaylistVideos_readsLockupViewModel() {
        val payload = """
            [
              {
                "lockupViewModel": {
                  "contentId": "abcdefghijk",
                  "metadata": {
                    "lockupMetadataViewModel": {
                      "title": { "content": "Lockup video" }
                    }
                  },
                  "contentImage": {
                    "thumbnailViewModel": {
                      "overlays": [
                        {
                          "thumbnailOverlayBadgeViewModel": {
                            "thumbnailBadges": [
                              { "thumbnailBadgeViewModel": { "text": "1:02:03" } }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
              }
            ]
        """.trimIndent()
        val items = json.parseToJsonElement(payload).jsonArray
        val (videos, continuation) = YouTubePlaylistExtractor.parsePlaylistVideos(items)

        assertEquals(1, videos.size)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", videos[0].url)
        assertEquals("Lockup video", videos[0].title)
        assertEquals("1:02:03", videos[0].duration)
        assertEquals("abcdefghijk", videos[0].videoId)
        assertEquals(null, continuation)
    }

    @Test
    fun parseClassicVideo_fallsBackToSimpleTextTitle() {
        val payload = """
            [
              {
                "playlistVideoRenderer": {
                  "videoId": "simple12345",
                  "title": { "simpleText": "Simple title" }
                }
              }
            ]
        """.trimIndent()
        val items = json.parseToJsonElement(payload).jsonArray
        val (videos, _) = YouTubePlaylistExtractor.parsePlaylistVideos(items)
        assertEquals("Simple title", videos.single().title)
        assertEquals("simple12345", videos.single().videoId)
    }

    @Test
    fun normalizeUrl_convertsWatchListToPlaylist() {
        val url = "https://www.youtube.com/watch?v=abc&list=PLxyz&index=2"
        val normalized = YouTubePlaylistExtractor.normalizeUrl(url)
        assertEquals("https://www.youtube.com/playlist?list=PLxyz", normalized)
    }

    @Test
    fun channelIdToUploadsPlaylistUrl_swapsUcPrefix() {
        val url = YouTubePlaylistExtractor.channelIdToUploadsPlaylistUrl("UCabcdefgh")
        assertEquals("https://www.youtube.com/playlist?list=UUabcdefgh", url)
    }

    @Test
    fun isChannelUrl_detectsHandleAndChannelPaths() {
        assertTrue(YouTubePlaylistExtractor.isChannelUrl("https://www.youtube.com/@someone"))
        assertTrue(YouTubePlaylistExtractor.isChannelUrl("https://www.youtube.com/channel/UCabc"))
        assertTrue(!YouTubePlaylistExtractor.isChannelUrl("https://www.youtube.com/playlist?list=PLxyz"))
    }
}
