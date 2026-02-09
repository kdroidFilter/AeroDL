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
    fun diagnose_detectsYouTubeAuthenticationRequirement() {
        val diagnostic = NetAndArchive.diagnose(
            listOf(
                "[youtube] FMqiGAXHVNs: Downloading ios player API JSON",
                "ERROR: [youtube] FMqiGAXHVNs: Sign in to confirm you're not a bot. Use --cookies-from-browser or --cookies for the authentication.",
            ),
        )

        assertNotNull(diagnostic)
        assertTrue(diagnostic.contains("Authentication required"))
        assertTrue(diagnostic.contains("cookies", ignoreCase = true))
    }

    @Test
    fun youtubeAuthenticationIssue_detectsMembersOnlyAndAgeRestrictedSignals() {
        val membersOnlyLines = listOf(
            "ERROR: This video is members-only. Join this channel to get access.",
            "ERROR: Use --cookies-from-browser for the authentication.",
        )
        val ageRestrictedLines = listOf(
            "ERROR: This video may be inappropriate for some users.",
            "Sign in to confirm your age",
        )

        assertTrue(NetAndArchive.isYouTubeAuthenticationIssue(membersOnlyLines))
        assertTrue(NetAndArchive.isYouTubeAuthenticationIssue(ageRestrictedLines))
        assertTrue(NetAndArchive.diagnose(membersOnlyLines)?.contains("Authentication required") == true)
        assertTrue(NetAndArchive.diagnose(ageRestrictedLines)?.contains("Authentication required") == true)
    }

    @Test
    fun diagnose_detectsYouTubeExtractorMismatch() {
        val lines = listOf(
            "[youtube] abc123: Downloading player API JSON",
            "WARNING: [youtube] abc123: nsig extraction failed: hash mismatch while solving challenge",
            "ERROR: Signature extraction failed: Some formats may be missing",
        )

        val diagnostic = NetAndArchive.diagnose(lines)

        assertNotNull(diagnostic)
        assertTrue(diagnostic.contains("extractor", ignoreCase = true))
        assertTrue(NetAndArchive.isYouTubeExtractorIssue(lines))
        assertTrue(!NetAndArchive.isYouTubeAuthenticationIssue(lines))
    }

    @Test
    fun diagnose_prioritizesNetworkIssuesWhenMultipleSignalsExist() {
        val lines = listOf(
            "ERROR: Read timed out while downloading webpage",
            "ERROR: Sign in to confirm you're not a bot",
        )

        val diagnostic = NetAndArchive.diagnose(lines)

        assertNotNull(diagnostic)
        assertTrue(diagnostic.contains("Network timeout"))
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

    @Test
    fun selectors_containFallbackFormats() {
        // Progressive selector should have universal fallback
        val selProg = NetAndArchive.selectorProgressiveExact(720)
        assertTrue(selProg.contains("height<=720"), "Progressive selector should have height<= fallback")
        assertTrue(selProg.contains("bestvideo+bestaudio/best"), "Progressive selector should have universal fallback")

        // Download selector should have height<= fallback
        val selDl = NetAndArchive.selectorDownloadExact(1080)
        assertTrue(selDl.contains("height<=1080"), "Download selector should have height<= fallback")

        // MP4 selector should have height<= fallback and universal fallback
        val selMp4 = NetAndArchive.selectorDownloadExactMp4(480)
        assertTrue(selMp4.contains("height<=480"), "MP4 selector should have height<= fallback")
        assertTrue(selMp4.contains("bestvideo+bestaudio/best"), "MP4 selector should have universal fallback")
    }
}
