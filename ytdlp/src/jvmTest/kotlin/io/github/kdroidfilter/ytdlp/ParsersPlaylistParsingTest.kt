package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.parsePlaylistInfoFromJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParsersPlaylistParsingTest {
    @Test
    fun parsePlaylistInfoFromJson_extractsEntriesAndThumbnail() {
        val json = """
            {
              "_type": "playlist",
              "id": "PL12345ABC",
              "title": "My Playlist",
              "webpage_url": "https://www.youtube.com/playlist?list=PL12345ABC",
              "entries": [
                {"id":"vid1","title":"Video 1","webpage_url":"https://www.youtube.com/watch?v=aaaaaaaaaaa"},
                {"id":"vid2","title":"Video 2","webpage_url":"https://www.youtube.com/watch?v=bbbbbbbbbbb"}
              ]
            }
        """.trimIndent()

        val playlist = parsePlaylistInfoFromJson(json)
        assertEquals("PL12345ABC", playlist.id)
        assertEquals("My Playlist", playlist.title)
        assertEquals(2, playlist.entries.size)
        // Expect a best-effort thumbnail (from first entry or computed for playlist)
        assertNotNull(playlist.thumbnail)
        assertTrue(playlist.entryCount == 2)
    }
}

