package io.github.kdroidfilter.ytdlp.core

import java.time.Duration

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
    val subtitles: SubtitleOptions? = null,  // NEW: Subtitle configuration
    val sponsorBlockRemove: Boolean = false  // NEW: SponsorBlock segment removal
)