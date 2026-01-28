package io.github.kdroidfilter.ffmpeg

import io.github.kdroidfilter.ffmpeg.util.ErrorDiagnostics
import io.github.kdroidfilter.ffmpeg.util.ProgressParser
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProgressParserTest {

    @Test
    fun `parse basic progress line`() {
        val line = "frame=  500 fps=30.0 q=28.0 size=    1024kB time=00:00:16.67 bitrate= 502.4kbits/s speed=1.00x"
        val progress = ProgressParser.parse(line)

        assertNotNull(progress)
        assertEquals(Duration.ofMillis(16670), progress.time)
        assertEquals(1.0, progress.speed)
        assertEquals(30.0, progress.fps)
        assertNotNull(progress.bitrate)
    }

    @Test
    fun `parse progress with different time formats`() {
        val line1 = "time=01:30:45.50 speed=2.5x"
        val progress1 = ProgressParser.parse(line1)
        assertNotNull(progress1)
        assertEquals(Duration.ofHours(1).plusMinutes(30).plusSeconds(45).plusMillis(500), progress1.time)
        assertEquals(2.5, progress1.speed)

        val line2 = "time=00:05:30.00 speed=0.8x"
        val progress2 = ProgressParser.parse(line2)
        assertNotNull(progress2)
        assertEquals(Duration.ofMinutes(5).plusSeconds(30), progress2.time)
        assertEquals(0.8, progress2.speed)
    }

    @Test
    fun `parse progress without speed`() {
        val line = "frame=100 time=00:00:10.00"
        val progress = ProgressParser.parse(line)

        assertNotNull(progress)
        assertEquals(Duration.ofSeconds(10), progress.time)
        assertNull(progress.speed)
    }

    @Test
    fun `return null for non-progress line`() {
        val lines = listOf(
            "Input #0, matroska,webm, from 'input.mkv':",
            "  Duration: 01:30:00.00, start: 0.000000, bitrate: 5000 kb/s",
            "Stream #0:0: Video: h264 (High)",
            "Press [q] to stop, [?] for help"
        )

        lines.forEach { line ->
            assertNull(ProgressParser.parse(line))
        }
    }

    @Test
    fun `calculate percent correctly`() {
        val processed = Duration.ofMinutes(5)
        val total = Duration.ofMinutes(10)

        val percent = ProgressParser.calculatePercent(processed, total)
        assertEquals(50.0, percent)
    }

    @Test
    fun `calculate percent returns null for zero duration`() {
        val processed = Duration.ofMinutes(5)
        val total = Duration.ZERO

        assertNull(ProgressParser.calculatePercent(processed, total))
    }

    @Test
    fun `calculate percent caps at 100`() {
        val processed = Duration.ofMinutes(15)
        val total = Duration.ofMinutes(10)

        val percent = ProgressParser.calculatePercent(processed, total)
        assertEquals(100.0, percent)
    }

    @Test
    fun `parse bitrate correctly`() {
        val line = "time=00:01:00.00 bitrate=1500.5kbits/s speed=1.0x"
        val progress = ProgressParser.parse(line)

        assertNotNull(progress)
        assertNotNull(progress.bitrate)
        assertEquals(1500500L, progress.bitrate)
    }

    @Test
    fun `diagnose file not found error`() {
        val lines = listOf(
            "ffmpeg version 5.1.2",
            "/input/file.mkv: No such file or directory"
        )

        val diagnosis = ErrorDiagnostics.diagnose(lines)
        assertEquals("Input file not found", diagnosis)
    }

    @Test
    fun `diagnose encoder not found error`() {
        val lines = listOf(
            "Unknown encoder 'libx265'"
        )

        val diagnosis = ErrorDiagnostics.diagnose(lines)
        assertEquals("Encoder not available (may need to be compiled into FFmpeg)", diagnosis)
    }

    @Test
    fun `diagnose permission denied error`() {
        val lines = listOf(
            "Permission denied"
        )

        val diagnosis = ErrorDiagnostics.diagnose(lines)
        assertEquals("Permission denied accessing file", diagnosis)
    }

    @Test
    fun `diagnose returns null for unknown error`() {
        val lines = listOf(
            "Some random output",
            "That doesn't match any pattern"
        )

        assertNull(ErrorDiagnostics.diagnose(lines))
    }
}
