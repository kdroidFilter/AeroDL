package io.github.kdroidfilter.ytdlp.core

import java.time.Duration

/**
 * Data class to hold configuration options for a yt-dlp command.
 *
 * @property format The format selector string (e.g., "bestvideo+bestaudio/best").
 * @property outputTemplate The output file name template (e.g., "%(title)s.%(ext)s").
 * @property noCheckCertificate If true, disables SSL certificate validation.
 * @property extraArgs A list of additional command-line arguments to pass to yt-dlp.
 * @property timeout The maximum duration for the download process. Null for no timeout.
 * @property targetContainer The desired container format (e.g., "mp4"). Null to keep the original.
 * @property allowRecode If true, allows re-encoding if remuxing into the target container is not possible.
 */
data class Options(
    val format: String? = null,
    val outputTemplate: String? = null,
    val noCheckCertificate: Boolean = false,
    val extraArgs: List<String> = emptyList(),
    val timeout: Duration? = Duration.ofMinutes(30),
    val targetContainer: String? = null,
    val allowRecode: Boolean = false,
)