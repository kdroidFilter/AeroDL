package io.github.kdroidfilter.ytdlp.core

import java.time.Duration

/**
 * Defines a time range to download from a video.
 * Uses yt-dlp's --download-sections format.
 *
 * @property startSeconds The start time in seconds
 * @property endSeconds The end time in seconds
 */
data class DownloadSection(
    val startSeconds: Double,
    val endSeconds: Double
) {
    /**
     * Formats the section for yt-dlp's --download-sections argument.
     * Format: "*START-END" where times are in HH:MM:SS format.
     */
    fun toYtDlpFormat(): String {
        return "*${formatTime(startSeconds)}-${formatTime(endSeconds)}"
    }

    private fun formatTime(seconds: Double): String {
        val totalSec = seconds.toLong()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}

/**
 * Configuration for subtitle downloading
 */
data class SubtitleOptions(
    val languages: List<String> = emptyList(),  // List of language codes (e.g., ["en", "fr", "es"])
    val writeSubtitles: Boolean = true,         // Write subtitle files
    val embedSubtitles: Boolean = true,         // Embed subtitles in the video file
    val writeAutoSubtitles: Boolean = false,    // Include auto-generated subtitles
    val subFormat: String? = null,              // Preferred subtitle format (e.g., "srt", "vtt", "ass")
    val allSubtitles: Boolean = false,          // Download all available subtitles
    val convertSubtitles: String? = null        // Convert subtitles to this format
)

/**
 * Data class to hold configuration options for a yt-dlp command.
 *
 * @property format The format selector string (e.g., "bestvideo+bestaudio/best").
 * @property outputTemplate The output file name template (e.g., "%(title)s.%(ext)s").
 * @property noCheckCertificate If true, disables SSL certificate validation.
 * @property cookiesFromBrowser Name of the browser to import cookies from (e.g., "firefox"). Null to disable.
 * @property extraArgs A list of additional command-line arguments to pass to yt-dlp.
 * @property timeout The maximum duration for the download process. Null for no timeout.
 * @property targetContainer The desired container format (e.g., "mp4"). Null to keep the original.
 * @property allowRecode If true, allows re-encoding if remuxing into the target container is not possible.
 * @property subtitles Subtitle download configuration. Null to skip subtitle downloading.
 * @property sponsorBlockRemove If true, removes SponsorBlock segments (default categories).
 * @property concurrentFragments Number of threads for downloading m3u8/mpd fragments in parallel (1-5). 1 = disabled.
 * @property proxy Proxy URL to use for the download (e.g., "http://127.0.0.1:8080", "socks5://127.0.0.1:1080"). Null to disable.
 * @property downloadSection Time range to download (trim/cut). Null to download the entire video.
 * @property useHardwareAcceleration If true, uses hardware acceleration for re-encoding when available.
 */
data class Options(
    val format: String? = null,
    val outputTemplate: String? = null,
    val noCheckCertificate: Boolean = false,
    val cookiesFromBrowser: String? = null,
    val extraArgs: List<String> = emptyList(),
    val timeout: Duration? = Duration.ofMinutes(30),
    val targetContainer: String? = null,
    val allowRecode: Boolean = false,
    val subtitles: SubtitleOptions? = null,
    val sponsorBlockRemove: Boolean = false,
    val concurrentFragments: Int = 1,
    val proxy: String? = null,
    val downloadSection: DownloadSection? = null,
    val useHardwareAcceleration: Boolean = true
)