package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.ytdlp.core.Options
import java.io.BufferedInputStream
import java.io.File
import java.net.*
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
            val url = URL(targetUrl)
            try { InetAddress.getByName(url.host) } catch (e: UnknownHostException) {
                return Result.failure(IllegalStateException("DNS resolution failed for ${url.host}", e))
            }

            (url.openConnection() as URLConnection).apply {
                if (this is HttpURLConnection) {
                    requestMethod = "HEAD"
                    instanceFollowRedirects = true
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    setRequestProperty("User-Agent", "Mozilla/5.0 (YtDlpWrapper)")
                    connect()
                    if (responseCode in 200..399) {
                        disconnect()
                        return Result.success(Unit)
                    }
                }
            }

            // Fallback check to a known reliable host if the target host fails, to distinguish
            // between a general network problem and a specific host being down.
            val fallback = URL("https://www.gstatic.com/generate_204")
            (fallback.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                connect(); disconnect()
            }
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

        ffmpegPath?.takeIf { it.isNotBlank() }?.let { cmd.addAll(listOf("--ffmpeg-location", it)) }
        if (options.noCheckCertificate) cmd.add("--no-check-certificate")

        downloadDir?.let { dir ->
            if (!dir.exists()) dir.mkdirs()
            val tpl = options.outputTemplate ?: "%(title)s.%(ext)s"
            cmd.addAll(listOf("-o", File(dir, tpl).absolutePath))
        } ?: run {
            options.outputTemplate?.let { tpl -> cmd.addAll(listOf("-o", tpl)) }
        }

        options.format?.let { cmd.addAll(listOf("-f", it)) }

        // Post-processing to enforce a container if requested
        options.targetContainer?.let { container ->
            if (container.equals("mp4", ignoreCase = true)) {
                if (options.allowRecode) {
                    // Slow but guaranteed: re-encode to MP4 if remuxing is impossible
                    cmd.addAll(listOf("--recode-video", "mp4"))
                } else {
                    // Fast: remux only (no quality loss). Will fail if codecs are incompatible with mp4.
                    cmd.addAll(listOf("--remux-video", "mp4"))
                }
            } else {
                // For other containers
                if (options.allowRecode) cmd.addAll(listOf("--recode-video", container))
                else cmd.addAll(listOf("--remux-video", container))
            }
        }

        if (options.extraArgs.isNotEmpty()) cmd.addAll(options.extraArgs)
        cmd.add(url)
        return cmd
    }

    // --- Progress Parsing ---
    private val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
    fun parseProgress(line: String): Double? =
        percentRegex.find(line)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()

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

    /** A very small, robust `-F` parser to detect available heights (progressive vs. video-only). */
    fun probeAvailableHeights(formatListOutput: List<String>): Pair<Set<Int>, Set<Int>> {
        val heightRegex = Regex("""\b(\d{3,4})p\b""")
        val progressive = mutableSetOf<Int>()
        val videoOnly = mutableSetOf<Int>()
        for (line in formatListOutput) {
            val h = heightRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: continue
            val lower = line.lowercase()
            when {
                "audio only" in lower -> {} // ignore
                "video only" in lower -> videoOnly += h
                else -> progressive += h
            }
        }
        return progressive to videoOnly
    }

    // Prefer MP4/M4A at exact height (falls back if not available)
    fun selectorDownloadExactMp4(height: Int): String {
        val splitPreferMp4 = "bestvideo[height=$height][ext=mp4]+bestaudio[ext=m4a]/" +
                "bestvideo[height=$height]+bestaudio"
        val progressiveMp4 = "best[height=$height][ext=mp4][acodec!=none][vcodec!=none][protocol!=m3u8]/" +
                "best[height=$height][acodec!=none][vcodec!=none][protocol!=m3u8]"
        return "$splitPreferMp4/$progressiveMp4"
    }

    /** Best progressive MP4 at or below maxHeight */
    fun progressiveMediumSelectorMp4(maxHeight: Int): String {
        val mp4 = "best[height<=${maxHeight}][ext=mp4][acodec!=none][vcodec!=none][protocol!=m3u8]"
        val any = "best[height<=${maxHeight}][acodec!=none][vcodec!=none][protocol!=m3u8]"
        return "$mp4/$any"
    }
}