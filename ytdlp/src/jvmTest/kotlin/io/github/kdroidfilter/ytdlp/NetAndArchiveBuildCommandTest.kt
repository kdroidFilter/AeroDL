package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.Options
import io.github.kdroidfilter.ytdlp.core.SubtitleOptions
import io.github.kdroidfilter.ytdlp.util.NetAndArchive
import kotlin.test.Test
import kotlin.test.assertTrue

class NetAndArchiveBuildCommandTest {

    @Test
    fun buildCommand_includesSubtitleFlags_specificLanguages() {
        val opts = Options(
            outputTemplate = "%(title)s.%(ext)s",
            subtitles = SubtitleOptions(
                languages = listOf("en", "fr"),
                writeSubtitles = false,
                embedSubtitles = true,
                writeAutoSubtitles = true,
                subFormat = "srt"
            )
        )
        val cmd = NetAndArchive.buildCommand(
            ytDlpPath = "/usr/bin/yt-dlp",
            ffmpegPath = null,
            url = "https://example.com/video",
            options = opts,
            downloadDir = null
        )
        assertTrue(cmd.contains("--embed-subs"))
        assertTrue(cmd.contains("--write-auto-subs"))
        assertTrue(cmd.contains("--sub-format"))
        assertTrue(cmd.contains("--sub-langs"))
        assertTrue(cmd.contains("--no-simulate"))
        assertTrue(cmd.contains("--print-to-file"))
    }

    @Test
    fun buildCommand_includesWriteSubs_whenRequested() {
        val opts = Options(
            outputTemplate = "%(title)s.%(ext)s",
            subtitles = SubtitleOptions(
                languages = listOf("de"),
                writeSubtitles = true,
                embedSubtitles = false,
                writeAutoSubtitles = false
            )
        )
        val cmd = NetAndArchive.buildCommand(
            ytDlpPath = "/usr/bin/yt-dlp",
            ffmpegPath = null,
            url = "https://example.com/video",
            options = opts,
            downloadDir = null
        )
        assertTrue(cmd.contains("--write-subs"))
        assertTrue(cmd.contains("--sub-langs"))
    }

    @Test
    fun buildCommand_remuxVsRecode_mp4() {
        val remux = Options(outputTemplate = "%(title)s.%(ext)s", targetContainer = "mp4", allowRecode = false)
        val recode = Options(outputTemplate = "%(title)s.%(ext)s", targetContainer = "mp4", allowRecode = true)

        val cmdRemux = NetAndArchive.buildCommand("/usr/bin/yt-dlp", null, "https://example.com/video", remux, null)
        val cmdRecode = NetAndArchive.buildCommand("/usr/bin/yt-dlp", null, "https://example.com/video", recode, null)

        assertTrue(cmdRemux.contains("--remux-video"))
        assertTrue(cmdRecode.contains("--recode-video"))
    }
}

