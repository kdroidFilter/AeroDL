package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.util.NetAndArchive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetAndArchiveUtilsTest {

    @Test
    fun parseProgress_extractsPercentFromVariousFormats() {
        assertEquals(12.5, NetAndArchive.parseProgress("[download]  12.5% of 10MiB at 2MiB/s"))
        assertEquals(99.0, NetAndArchive.parseProgress("[download]  99% of 10MiB at 2MiB/s"))
        assertEquals(100.0, NetAndArchive.parseProgress("[download]  100,0% of 10MiB at 2MiB/s"))
        assertEquals(null, NetAndArchive.parseProgress("no percent here"))
    }

    @Test
    fun parseSpeed_parsesBytesPerSecond() {
        // 2.0 MiB/s -> binary units
        assertEquals(2L * 1024L * 1024L, NetAndArchive.parseSpeedBytesPerSec("[download]  12.5% of 10MiB at 2.0MiB/s ETA 00:19"))
        // 850 KiB/s -> binary units
        assertEquals(850L * 1024L, NetAndArchive.parseSpeedBytesPerSec("[download]  99% of 2.0GiB at 850KiB/s"))
        // 1.2 MB/s -> decimal units
        assertEquals(1_200_000L, NetAndArchive.parseSpeedBytesPerSec("[download]  100,0% of ~ 100.0MiB at 1.2MB/s"))
        // No speed
        assertEquals(null, NetAndArchive.parseSpeedBytesPerSec("no speed here"))
    }

    @Test
    fun diagnose_returnsHelpfulMessages() {
        assertTrue(NetAndArchive.diagnose(listOf("Connection refused by host"))?.contains("Connection problem") == true)
        assertTrue(NetAndArchive.diagnose(listOf("Operation timed out"))?.contains("timeout", ignoreCase = true) == true)
        assertTrue(NetAndArchive.diagnose(listOf("Temporary failure in name resolution"))?.contains("DNS") == true)
        assertTrue(NetAndArchive.diagnose(listOf("SSL: certificate verify failed"))?.contains("TLS/Certificate") == true)
        assertTrue(NetAndArchive.diagnose(listOf("HTTP Error 429"))?.contains("Rate limited") == true)
        assertTrue(NetAndArchive.diagnose(listOf("HTTP Error 403"))?.contains("403") == true)
    }

    @Test
    fun selectors_containExpectedConstraints() {
        val selProg = NetAndArchive.selectorProgressiveExact(720)
        assertTrue(selProg.contains("height=720"))
        assertTrue(selProg.contains("acodec!=none"))
        assertTrue(selProg.contains("vcodec!=none"))
        assertTrue(selProg.contains("protocol!=m3u8"))

        val selDl = NetAndArchive.selectorDownloadExact(1080)
        assertTrue(selDl.contains("bestvideo[height=1080]"))
        assertTrue(selDl.contains("+bestaudio"))

        val selMp4 = NetAndArchive.selectorDownloadExactMp4(480)
        assertTrue(selMp4.contains("ext=mp4"))
        assertTrue(selMp4.contains("bestaudio[ext=m4a]"))
    }
}
