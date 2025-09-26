package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.ytdlp.core.Options
import java.io.BufferedInputStream
import java.io.File
import java.net.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

object NetAndArchive {

    // ---- ZIP / TAR.XZ ----
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

    // ---- Network preflight ----
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

    // ---- Command construction ----
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
        if (options.extraArgs.isNotEmpty()) cmd.addAll(options.extraArgs)
        cmd.add(url)
        return cmd
    }

    // ---- Progress parsing ----
    private val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
    fun parseProgress(line: String): Double? =
        percentRegex.find(line)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()

    // ---- Diagnosis ----
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

    // ---- Small utils ----
    fun startNoopProcess(): Process = try {
        val os = io.github.kdroidfilter.platformtools.getOperatingSystem()
        when (os) {
            io.github.kdroidfilter.platformtools.OperatingSystem.WINDOWS ->
                ProcessBuilder("cmd", "/c", "exit", "0").start()
            else -> ProcessBuilder("sh", "-c", "true").start()
        }
    } catch (_: Exception) {
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

    // ---- Selector helpers ----
    fun progressiveMediumSelector(
        maxHeight: Int = 480,
        preferredExts: List<String> = listOf("mp4", "webm")
    ): String {
        val common = "best[acodec!=none][vcodec!=none][height<=$maxHeight]"
        val biased = preferredExts.joinToString("/") { ext -> "$common[ext=$ext]" }
        return "$biased/$common"
    }
}
