package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.network.HttpsConnectionFactory
import io.github.kdroidfilter.ytdlp.core.Options
import io.github.kdroidfilter.ytdlp.core.SubtitleOptions
import io.github.kdroidfilter.logging.infoln
import java.io.BufferedInputStream
import java.io.File
import java.net.*
import java.security.MessageDigest
import java.util.zip.ZipInputStream

object NetAndArchive {

    // --- Archive Utilities ---
    fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(destDir, entry.name)
                if (entry.isDirectory) out.mkdirs()
                else { out.parentFile?.mkdirs(); out.outputStream().use { os -> zis.copyTo(os) } }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun extractTarXzWithSystemTar(archive: File, destDir: File) {
        val p = ProcessBuilder("tar", "-xJf", archive.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        if (code != 0) error("tar failed ($code): $out")
    }

    // --- Network Preflight Check ---
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int, readTimeoutMs: Int): Result<Unit> {
        return try {
            val url = URI(targetUrl).toURL()
            try { InetAddress.getByName(url.host) } catch (e: UnknownHostException) {
                return Result.failure(IllegalStateException("DNS resolution failed for ${url.host}", e))
            }

            val conn = HttpsConnectionFactory.openConnection(url) {
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("User-Agent", "Mozilla/5.0 (YtDlpWrapper)")
            }
            conn.connect()
            if (conn.responseCode in 200..399) {
                conn.disconnect()
                return Result.success(Unit)
            }
            conn.disconnect()

            // Fallback check to a known reliable host if the target host fails, to distinguish
            // between a general network problem and a specific host being down.
            val fallback = URI("https://www.gstatic.com/generate_204").toURL()
            val fallbackConn = HttpsConnectionFactory.openConnection(fallback) {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
            }
            fallbackConn.connect()
            fallbackConn.disconnect()
            Result.success(Unit)
        } catch (e: SocketTimeoutException) {
            Result.failure(IllegalStateException("Network timeout", e))
        } catch (e: ConnectException) {
            Result.failure(IllegalStateException("No route to host / connection refused", e))
        } catch (e: UnknownHostException) {
            Result.failure(IllegalStateException("DNS resolution failed", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Command Construction ---
    fun buildCommand(
        ytDlpPath: String,
        ffmpegPath: String?,
        url: String,
        options: Options,
        downloadDir: File?
    ): List<String> {
        val cmd = mutableListOf(ytDlpPath, "--newline")

        // Point yt-dlp to the directory that contains ffmpeg AND ffprobe.
        // Passing the directory ensures yt-dlp can locate both tools reliably.
        ffmpegPath?.takeIf { it.isNotBlank() }?.let {
            val location = File(it).parentFile?.absolutePath ?: it
            cmd.addAll(listOf("--ffmpeg-location", location))
        }
        if (options.noCheckCertificate) cmd.add("--no-check-certificate")
        options.cookiesFromBrowser?.takeIf { it.isNotBlank() }?.let { cmd.addAll(listOf("--cookies-from-browser", it)) }

        // Detect if caller provided a chapter-only output via extra args.
        // If so, suppress the base "-o" to avoid creating a non-split file.
        val (hasChapterOutput, hasNonChapterOutput) = run {
            var chapterO = false
            var nonChapterO = false
            val args = options.extraArgs
            var i = 0
            while (i < args.size) {
                val a = args[i]
                if (a == "-o" || a == "--output") {
                    val b = args.getOrNull(i + 1) ?: ""
                    if (b.startsWith("chapter:")) chapterO = true else nonChapterO = true
                    i += 2
                } else i++
            }
            Pair(chapterO, nonChapterO)
        }
        val suppressBaseOutput = hasChapterOutput && !hasNonChapterOutput

        // Output template (skip base when only chapter output is specified)
        if (!suppressBaseOutput) {
            downloadDir?.let { dir ->
                if (!dir.exists()) dir.mkdirs()
                val tpl = options.outputTemplate ?: "%(title)s.%(ext)s"
                cmd.addAll(listOf("-o", File(dir, tpl).absolutePath))
            } ?: run {
                options.outputTemplate?.let { tpl -> cmd.addAll(listOf("-o", tpl)) }
            }
        }

        options.format?.let { cmd.addAll(listOf("-f", it)) }

        // Subtitles
        options.subtitles?.let { subOpts -> handleSubtitleOptions(cmd, subOpts) }

        // SponsorBlock
        if (options.sponsorBlockRemove) {
            cmd.addAll(listOf("--sponsorblock-remove", "default"))
        }

        // Concurrent fragments
        if (options.concurrentFragments > 1) {
            cmd.addAll(listOf("--concurrent-fragments", options.concurrentFragments.coerceIn(1, 5).toString()))
        }

        // Container enforcement (kept)
        options.targetContainer?.let { container ->
            if (container.equals("mp4", ignoreCase = true)) {
                if (options.allowRecode) cmd.addAll(listOf("--recode-video", "mp4"))
                else cmd.addAll(listOf("--remux-video", "mp4"))
            } else {
                if (options.allowRecode) cmd.addAll(listOf("--recode-video", container))
                else cmd.addAll(listOf("--remux-video", container))
            }
        }

        // --- NEW: Write the final absolute path to a temp file (no shell redirection)
        // File path is deterministic: %TEMP%/ytdlp-finalpath-<md5(url)>.txt
        val sinkFile = File(System.getProperty("java.io.tmpdir"),
            "ytdlp-finalpath-${md5(url)}.txt"
        )
        // Ensure we are not in simulate mode so after_move actually runs
        cmd.add("--no-simulate")
        // Append final path to sink file (UTF-8/locale handled by Python; avoids console codepage)
        // Syntax: --print-to-file [WHEN:]TEMPLATE FILE
        cmd.addAll(listOf(
            "--print-to-file", "after_move:%(filepath)s", sinkFile.absolutePath
        ))

        // Extra args passthrough
        if (options.extraArgs.isNotEmpty()) cmd.addAll(options.extraArgs)

        cmd.add(url)
        return cmd
    }

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    /**
     * Add subtitle-related arguments to the command
     */
    private fun handleSubtitleOptions(cmd: MutableList<String>, subOpts: SubtitleOptions) {
        infoln { "[SubtitleOptions] Processing subtitle configuration: languages=${subOpts.languages}, allSubtitles=${subOpts.allSubtitles}, embed=${subOpts.embedSubtitles}, write=${subOpts.writeSubtitles}, writeAuto=${subOpts.writeAutoSubtitles}" }

        when {
            // Download all subtitles
            subOpts.allSubtitles -> {
                infoln { "[SubtitleOptions] Requesting all available subtitles" }
                if (subOpts.writeSubtitles || !subOpts.embedSubtitles) {
                    cmd.add("--all-subs")
                } else {
                    // Embed only - download temporarily without keeping files
                    cmd.add("--all-subs")
                }
                if (subOpts.writeAutoSubtitles) {
                    infoln { "[SubtitleOptions] Including auto-generated subtitles" }
                    cmd.add("--write-auto-subs")
                }
            }
            // Download specific languages
            subOpts.languages.isNotEmpty() -> {
                infoln { "[SubtitleOptions] Requesting specific subtitle languages: ${subOpts.languages.joinToString(",")}" }
                // Only add --write-subs if we want to keep separate files
                if (subOpts.writeSubtitles || !subOpts.embedSubtitles) {
                    cmd.add("--write-subs")
                    infoln { "[SubtitleOptions] Added --write-subs flag" }
                }
                cmd.addAll(listOf("--sub-langs", subOpts.languages.joinToString(",")))
                if (subOpts.writeAutoSubtitles) {
                    infoln { "[SubtitleOptions] Including auto-generated subtitles for specified languages" }
                    cmd.add("--write-auto-subs")
                }
            }
            // Only auto-subs requested without specific languages
            subOpts.writeAutoSubtitles -> {
                infoln { "[SubtitleOptions] Requesting auto-generated subtitles only" }
                if (subOpts.writeSubtitles || !subOpts.embedSubtitles) {
                    cmd.add("--write-auto-subs")
                }
            }
        }

        // Embed subtitles in the video file (requires ffmpeg)
        if (subOpts.embedSubtitles) {
            infoln { "[SubtitleOptions] Embedding subtitles into video file (requires ffmpeg)" }
            cmd.add("--embed-subs")
        }

        // Specify subtitle format preference
        subOpts.subFormat?.let {
            infoln { "[SubtitleOptions] Subtitle format preference: $it" }
            cmd.addAll(listOf("--sub-format", it))
        }

        // Convert subtitles to a specific format after download
        subOpts.convertSubtitles?.let {
            infoln { "[SubtitleOptions] Will convert subtitles to: $it" }
            cmd.addAll(listOf("--convert-subs", it))
        }
    }

    // --- Progress Parsing ---
    private val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
    fun parseProgress(line: String): Double? =
        percentRegex.find(line)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()

    // Extracts instantaneous speed (bytes/sec) from typical yt-dlp progress lines.
    // Examples handled:
    //   "[download]  12.5% of 10MiB at 2.43MiB/s ETA 00:19"
    //   "[download]  99% of 2.0GiB at 850KiB/s"
    //   "[download]  100,0% of ~ 100.0MiB at 1.2MiB/s"
    private val NBSP: Char = '\u00A0'
    // Match speed with or without the literal "at" before it
    private val speedRegex = Regex("(?:\\bat\\s+)?[$NBSP\\s]*([\\d.,]+)[$NBSP\\s]*([KMGTP]?i?B)/s\\b", RegexOption.IGNORE_CASE)
    fun parseSpeedBytesPerSec(line: String): Long? {
        val m = speedRegex.find(line) ?: return null
        val number = m.groupValues.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull() ?: return null
        val unit = (m.groupValues.getOrNull(2) ?: "").uppercase()
        val multiplier = when (unit) {
            "B" -> 1.0
            "KB" -> 1_000.0
            "KIB" -> 1024.0
            "MB" -> 1_000_000.0
            "MIB" -> 1024.0 * 1024.0
            "GB" -> 1_000_000_000.0
            "GIB" -> 1024.0 * 1024.0 * 1024.0
            "TB" -> 1_000_000_000_000.0
            "TIB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
            "PB" -> 1_000_000_000_000_000.0
            "PIB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0
            else -> 1.0
        }
        val bps = number * multiplier
        return if (bps.isFinite() && bps >= 0.0) bps.toLong() else null
    }

    // --- Error Diagnosis ---
    fun diagnose(lines: List<String>): String? {
        val joined = lines.joinToString("\n").lowercase()
        fun has(vararg needles: String) = needles.any { joined.contains(it.lowercase()) }
        return when {
            has("connection refused", "no route to host", "network is unreachable") -> "Connection problem to remote host."
            has("timed out", "timeout", "operation timed out", "read timed out") -> "Network timeout."
            has("unknown host", "name or service not known", "temporary failure in name resolution") -> "DNS resolution failed."
            has("ssl: certificate verify failed", "self signed certificate", "certificate has expired") -> "TLS/Certificate problem (try --no-check-certificate if appropriate)."
            has("http error 403") -> "HTTP 403 Forbidden (access denied)."
            has("http error 429", "too many requests", "rate limited") -> "Rate limited (HTTP 429)."
            has("copyright", "unavailable", "this video is not available") -> "Content not available or restricted."
            has("proxy", "socks", "http proxy") -> "Proxy/network configuration error."
            else -> null
        }
    }

    // --- Small Utilities ---
    fun startNoopProcess(): Process = try {
        val os = io.github.kdroidfilter.platformtools.getOperatingSystem()
        when (os) {
            io.github.kdroidfilter.platformtools.OperatingSystem.WINDOWS ->
                ProcessBuilder("cmd", "/c", "exit", "0").start()
            else -> ProcessBuilder("sh", "-c", "true").start()
        }
    } catch (_: Exception) {
        // Fallback if the simple commands fail for some reason
        try {
            val os = io.github.kdroidfilter.platformtools.getOperatingSystem()
            when (os) {
                io.github.kdroidfilter.platformtools.OperatingSystem.WINDOWS ->
                    ProcessBuilder("cmd", "/c", "ver").start()
                else -> ProcessBuilder("sh", "-c", ":").start()
            }
        } catch (_: Exception) {
            ProcessBuilder("java", "-version").start()
        }
    }

    // --- Resolution Helpers ---

    /** Exact progressive selector (single A+V URL only). Falls back to progressive at same height if ext differs. */
    fun selectorProgressiveExact(height: Int, preferredExts: List<String> = listOf("mp4","webm")): String {
        val common = "best[height=$height][acodec!=none][vcodec!=none][protocol!=m3u8]"
        val biased = preferredExts.joinToString("/") { ext -> "$common[ext=$ext]" }
        return "$biased/$common"
    }

    /** Download selector: prefers split A/V at exact height (yt-dlp will merge), falls back to progressive exact. */
    fun selectorDownloadExact(height: Int, preferredExts: List<String> = listOf("mp4","webm")): String {
        val split = "bestvideo[height=$height][vcodec!=none]+bestaudio/bestvideo[height=$height]+bestaudio"
        val progressive = selectorProgressiveExact(height, preferredExts)
        return "$split/$progressive"
    }

    // Prefer MP4/M4A at exact height (falls back if not available)
    fun selectorDownloadExactMp4(height: Int): String {
        val splitPreferMp4 = "bestvideo[height=$height][ext=mp4]+bestaudio[ext=m4a]/" +
                "bestvideo[height=$height]+bestaudio"
        val progressiveMp4 = "best[height=$height][ext=mp4][acodec!=none][vcodec!=none][protocol!=m3u8]/" +
                "best[height=$height][acodec!=none][vcodec!=none][protocol!=m3u8]"
        return "$splitPreferMp4/$progressiveMp4"
    }
}
