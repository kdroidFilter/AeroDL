package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getCacheDir
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream
import kotlin.math.min

/**
 * Thin Kotlin wrapper around yt-dlp with:
 * - Self-update/download of yt-dlp from GitHub releases
 * - Optional auto-download of FFmpeg (Windows/Linux) from yt-dlp/FFmpeg-Builds latest assets
 * - macOS uses PATH-only FFmpeg (no official macOS build in that repo)
 * - Configurable download directory and extra arguments
 * - Streaming of progress lines via callbacks
 * - Strong error reporting (network checks, exit-code summary, tail buffer of logs)
 *
 * NOTE: All code comments are in English per user preference.
 */
class YtDlpWrapper {

    // ===== GitHub fetcher for yt-dlp proper =====
    private val ytdlpFetcher = GitHubReleaseFetcher(
        owner = "yt-dlp",
        repo = "yt-dlp"
    )

    // ===== User configuration (can be overridden externally) =====
    var ytDlpPath: String = getDefaultBinaryPath()
    var ffmpegPath: String? = null
    var downloadDir: File? = null

    companion object {
        /** Default cache path for yt-dlp binary (inside app cache dir). */
        fun getDefaultBinaryPath(): String {
            val dir = getCacheDir()
            val os = getOperatingSystem()
            val binaryName = when (os) {
                OperatingSystem.WINDOWS -> "yt-dlp.exe"
                else -> "yt-dlp"
            }
            return File(dir, binaryName).absolutePath
        }

        /** Asset name for yt-dlp matching host OS/arch/musl. */
        fun getAssetNameForSystem(): String {
            val os = getOperatingSystem()
            val arch = (System.getProperty("os.arch") ?: "").lowercase()

            return when (os) {
                OperatingSystem.WINDOWS -> when {
                    arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_win_arm64.exe"
                    arch.contains("32") || (arch.contains("x86") && !arch.contains("64")) -> "yt-dlp_x86.exe"
                    else -> "yt-dlp.exe"
                }
                OperatingSystem.MACOS -> when {
                    arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_macos_arm64"
                    else -> "yt-dlp_macos"
                }
                OperatingSystem.LINUX -> {
                    val isMusl = try {
                        val p = Runtime.getRuntime().exec(arrayOf("ldd", "--version"))
                        val out = p.inputStream.bufferedReader().readText()
                        out.contains("musl")
                    } catch (_: Exception) { false }

                    when {
                        isMusl && (arch.contains("aarch64") || arch.contains("arm64")) -> "yt-dlp_musllinux_aarch64"
                        isMusl -> "yt-dlp_musllinux"
                        arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_linux_aarch64"
                        arch.contains("armv7") -> "yt-dlp_linux_armv7l"
                        else -> "yt-dlp_linux"
                    }
                }
                else -> "yt-dlp"
            }
        }
    }

    // ===== Public options for a single download =====
    data class Options(
        val format: String? = null,            // e.g., "bestvideo+bestaudio/best"
        val outputTemplate: String? = null,    // e.g., "%(title)s.%(ext)s"
        val noCheckCertificate: Boolean = false,
        val extraArgs: List<String> = emptyList(),
        val timeout: Duration? = Duration.ofMinutes(30) // max wall time for the process
    )

    // ===== Streaming events =====
    sealed class Event {
        data class Progress(val percent: Double?, val rawLine: String) : Event()
        data class Log(val line: String) : Event()
        data class Error(val message: String, val cause: Throwable? = null) : Event()
        data class Completed(val exitCode: Int, val success: Boolean) : Event()
        data object Cancelled : Event()
        data object Started : Event()
        data class NetworkProblem(val detail: String) : Event()
    }

    // ====== yt-dlp availability/version/update ======

    /** Returns yt-dlp version or null if unavailable. */
    fun version(): String? = try {
        val proc = ProcessBuilder(listOf(ytDlpPath, "--version"))
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val code = proc.waitFor()
        if (code == 0 && out.isNotBlank()) out else null
    } catch (_: Exception) {
        null
    }

    /** True if yt-dlp binary exists, is executable, and returns a version. */
    fun isAvailable(): Boolean {
        val file = File(ytDlpPath)
        return file.exists() && file.canExecute() && version() != null
    }

    /** True if remote latest version differs from local version (or yt-dlp is missing). */
    suspend fun hasUpdate(): Boolean {
        val currentVersion = version() ?: return true
        val latestVersion = ytdlpFetcher.getLatestRelease()?.tag_name ?: return false
        val current = currentVersion.removePrefix("v").trim()
        val latest = latestVersion.removePrefix("v").trim()
        return current != latest
    }

    /**
     * Download (or update) yt-dlp to the cache directory.
     * @return true if available afterwards.
     */
    suspend fun downloadOrUpdate(): Boolean {
        val os = getOperatingSystem()
        val assetName = getAssetNameForSystem()
        val destFile = File(getDefaultBinaryPath())
        if (destFile.parentFile?.exists() != true) destFile.parentFile?.mkdirs()

        return try {
            val release = ytdlpFetcher.getLatestRelease() ?: return false
            val asset = release.assets.find { it.name == assetName } ?: return false

            println("Downloading yt-dlp: ${asset.name} (${release.tag_name})")
            downloadFile(asset.browser_download_url, destFile)

            if (os != OperatingSystem.WINDOWS) makeExecutable(destFile)

            if (isAvailable()) {
                println("yt-dlp ready at ${destFile.absolutePath}")
                ytDlpPath = destFile.absolutePath
                true
            } else {
                println("Downloaded yt-dlp but it did not run")
                destFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ====== FFmpeg auto-download (Windows/Linux) ======

    /** Default FFmpeg path (inside cache/ffmpeg/bin/). */
    fun getDefaultFfmpegPath(): String {
        val dir = File(getCacheDir(), "ffmpeg/bin")
        val exe = if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg"
        return File(dir, exe).absolutePath
    }

    /** Try to locate ffmpeg in PATH using platform native command. */
    private fun findFfmpegInSystemPath(): String? {
        val cmd = if (getOperatingSystem() == OperatingSystem.WINDOWS)
            listOf("where", "ffmpeg") else listOf("which", "ffmpeg")
        return try {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.waitFor() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull()?.trim() else null
        } catch (_: Exception) { null }
    }

    /** Returns "ffmpeg -version" first line, or null if not runnable. */
    private fun ffmpegVersion(path: String): String? = try {
        val p = ProcessBuilder(listOf(path, "-version")).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText().trim()
        if (p.waitFor() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull() else null
    } catch (_: Exception) { null }

    /** Map expected FFmpeg asset name for current OS/arch (yt-dlp/FFmpeg-Builds 'gpl' static builds). */
    private fun getFfmpegAssetNameForSystem(): String? {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64")
        return when (os) {
            OperatingSystem.WINDOWS -> when {
                isArm64 -> "ffmpeg-master-latest-winarm64-gpl.zip"
                arch.contains("32") || (arch.contains("x86") && !arch.contains("64")) -> "ffmpeg-master-latest-win32-gpl.zip"
                else -> "ffmpeg-master-latest-win64-gpl.zip"
            }
            OperatingSystem.LINUX -> if (isArm64) "ffmpeg-master-latest-linuxarm64-gpl.tar.xz"
            else "ffmpeg-master-latest-linux64-gpl.tar.xz"
            OperatingSystem.MACOS -> null // no official macOS build in that repo
            else -> null
        }
    }

    /**
     * Ensure a working FFmpeg is available.
     * - If ffmpegPath is already set and runnable -> OK
     * - Else try PATH
     * - Else (Windows/Linux) auto-download from yt-dlp/FFmpeg-Builds 'latest'
     * - macOS: PATH only (repo doesn't ship macOS builds)
     */
    suspend fun ensureFfmpegAvailable(forceDownload: Boolean = false): Boolean {
        ffmpegPath?.let { if (ffmpegVersion(it) != null && !forceDownload) return true }
        findFfmpegInSystemPath()?.let { ffmpegPath = it; return true }
        return when (getOperatingSystem()) {
            OperatingSystem.WINDOWS, OperatingSystem.LINUX -> downloadFfmpeg(forceDownload)
            OperatingSystem.MACOS -> false
            else -> false
        }
    }

    /** Download & extract FFmpeg into cache/ffmpeg/bin/, set ffmpegPath. */
    suspend fun downloadFfmpeg(forceDownload: Boolean = false): Boolean {
        val asset = getFfmpegAssetNameForSystem() ?: return false
        val baseDir = File(getCacheDir(), "ffmpeg")
        val binDir = File(baseDir, "bin")
        val targetExe = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg")

        if (targetExe.exists() && ffmpegVersion(targetExe.absolutePath) != null && !forceDownload) {
            ffmpegPath = targetExe.absolutePath
            return true
        }

        baseDir.mkdirs(); binDir.mkdirs()
        val url = "https://github.com/yt-dlp/FFmpeg-Builds/releases/latest/download/$asset"
        val archive = File(baseDir, asset)

        return try {
            java.net.URI.create(url).toURL().openStream().use { ins ->
                Files.copy(ins, archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            if (asset.endsWith(".zip")) extractZip(archive, baseDir)
            else if (asset.endsWith(".tar.xz")) extractTarXzWithSystemTar(archive, baseDir)
            else error("Unsupported FFmpeg archive: $asset")

            val found = baseDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.startsWith("ffmpeg") && it.canRead() }
                ?: error("FFmpeg binary not found after extraction")

            found.copyTo(targetExe, overwrite = true)

            if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                try {
                    val perms = Files.getPosixFilePermissions(targetExe.toPath()).toMutableSet()
                    perms.add(PosixFilePermission.OWNER_EXECUTE)
                    perms.add(PosixFilePermission.GROUP_EXECUTE)
                    perms.add(PosixFilePermission.OTHERS_EXECUTE)
                    Files.setPosixFilePermissions(targetExe.toPath(), perms)
                } catch (_: UnsupportedOperationException) {
                    Runtime.getRuntime().exec(arrayOf("chmod", "+x", targetExe.absolutePath)).waitFor()
                }
            }

            ffmpegVersion(targetExe.absolutePath) ?: error("FFmpeg not runnable")
            ffmpegPath = targetExe.absolutePath
            true
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    /** Pure-Java ZIP extraction. */
    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
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

    /** Extract TAR.XZ via system `tar` (present on most Linux distros). */
    private fun extractTarXzWithSystemTar(archive: File, destDir: File) {
        val p = ProcessBuilder("tar", "-xJf", archive.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        if (code != 0) error("tar failed ($code): $out")
    }

    // ===== Network preflight =====

    /**
     * Quick network check before spawning yt-dlp to fail fast on no-connection cases.
     * - Tries to reach the target host with a HEAD request.
     * - Fallback to a lightweight known host if URL host cannot be resolved yet.
     */
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000): Result<Unit> {
        return try {
            val url = URL(targetUrl)
            // DNS check
            try { InetAddress.getByName(url.host) } catch (e: UnknownHostException) {
                return Result.failure(IllegalStateException("DNS resolution failed for ${url.host}", e))
            }

            // HEAD request to target (may be blocked; that's okay—we just test reachability)
            (url.openConnection() as URLConnection).apply {
                if (this is HttpURLConnection) {
                    requestMethod = "HEAD"
                    instanceFollowRedirects = true
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    setRequestProperty("User-Agent", "Mozilla/5.0 (YtDlpWrapper)")
                    connect()
                    // Accept any 2xx/3xx as “network OK”
                    if (responseCode in 200..399) {
                        disconnect()
                        return Result.success(Unit)
                    }
                }
            }

            // If we couldn't confirm, try a well-known fallback
            val fallback = URL("https://www.gstatic.com/generate_204")
            (fallback.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                connect()
                disconnect()
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

    // ====== Download execution =====

    data class Handle(
        val process: Process,
        /** Set to true to request cancellation (process will be destroyed). */
        val cancelled: AtomicBoolean
    ) {
        fun cancel() {
            cancelled.set(true)
            process.destroy()
        }
    }

    /**
     * Starts a yt-dlp download and streams the output lines to [onEvent].
     * Emits:
     *  - Started, Progress/Log lines, Completed(success=false) if exit!=0 with a summary Error,
     *  - Error early if process cannot start,
     *  - Cancelled if you call Handle.cancel().
     */
    fun download(
        url: String,
        options: Options = Options(),
        onEvent: (Event) -> Unit
    ): Handle {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

        // Fast network preflight
        val net = checkNetwork(url)
        if (net.isFailure) {
            onEvent(Event.NetworkProblem(net.exceptionOrNull()?.message ?: "Network not available"))
            onEvent(Event.Error("Network preflight failed."))
            // Create a fake handle that does nothing
            val proc = ProcessBuilder(listOf("true")).start()
            return Handle(proc, AtomicBoolean(true))
        }

        val cmd = buildCommand(url, options)
        val pb = ProcessBuilder(cmd)
        if (downloadDir != null) pb.directory(downloadDir)
        pb.redirectErrorStream(true)

        val process = try {
            pb.start()
        } catch (t: Throwable) {
            onEvent(Event.Error("Failed to start yt-dlp process (permissions or path issue).", t))
            // Fake handle
            val fake = ProcessBuilder(listOf("true")).start()
            return Handle(fake, AtomicBoolean(true))
        }

        val cancelled = AtomicBoolean(false)
        val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))

        // Keep a tail buffer of last N lines to help diagnose errors on non-zero exit.
        val tailCapacity = 120
        val tail = ArrayBlockingQueue<String>(tailCapacity)

        fun offerTail(line: String) {
            if (!tail.offer(line)) {
                tail.poll()
                tail.offer(line)
            }
        }

        onEvent(Event.Started)

        // Reader thread
        Thread {
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    offerTail(l)
                    val progress = parseProgress(l)
                    if (progress != null) onEvent(progress) else onEvent(Event.Log(l))
                }
            } catch (t: Throwable) {
                if (!cancelled.get()) onEvent(Event.Error("I/O error while reading yt-dlp output", t))
            } finally {
                try { reader.close() } catch (_: Exception) { }
            }
        }.apply {
            name = "yt-dlp-reader"
            isDaemon = true
        }.start()

        // Watchdog thread for timeout
        options.timeout?.let { limit ->
            Thread {
                val deadlineNs = System.nanoTime() + limit.toNanos()
                while (!cancelled.get()) {
                    if (System.nanoTime() > deadlineNs) {
                        cancelled.set(true)
                        process.destroy()
                        onEvent(Event.Error("Download timed out after ${limit.toMinutes()} minutes."))
                        break
                    }
                    try { Thread.sleep(300) } catch (_: InterruptedException) { break }
                }
            }.apply {
                name = "yt-dlp-timeout"
                isDaemon = true
            }.start()
        }

        // Completion watcher
        Thread {
            val exit = try { process.waitFor() } catch (_: InterruptedException) { -1 }
            val ok = (exit == 0)
            if (cancelled.get()) {
                onEvent(Event.Cancelled)
            } else if (!ok) {
                // Build a concise failure summary from tail lines.
                val lines = mutableListOf<String>()
                tail.drainTo(lines)
                val diagnostic = diagnose(lines)
                val tailPreview = if (lines.isEmpty()) "(no output captured)"
                else lines.takeLast(min(15, lines.size)).joinToString("\n")

                onEvent(
                    Event.Error(
                        "yt-dlp failed (exit $exit). ${diagnostic ?: ""}".trim()
                                + "\n--- Last output ---\n$tailPreview"
                    )
                )
            }
            onEvent(Event.Completed(exit, ok))
        }.apply {
            name = "yt-dlp-completion"
            isDaemon = true
        }.start()

        return Handle(process, cancelled)
    }

    // === Direct URL (no download) helpers =======================================

    /**
     * Build a format selector that favors single-file (progressive) muxed formats
     * (i.e., both audio and video in one container) up to [maxHeight].
     *
     * We avoid separate video/audio (which would trigger merging) by requiring
     * both acodec and vcodec to be present. We also bias to mp4, then webm.
     *
     * Example output:
     *  best[acodec!=none][vcodec!=none][ext=mp4][height<=480]/
     *  best[acodec!=none][vcodec!=none][ext=webm][height<=480]/
     *  best[acodec!=none][vcodec!=none][height<=480]
     */
    private fun progressiveMediumSelector(
        maxHeight: Int = 480,
        preferredExts: List<String> = listOf("mp4", "webm")
    ): String {
        val common = "best[acodec!=none][vcodec!=none][height<=$maxHeight]"
        val biased = preferredExts.joinToString(separator = "/") { ext ->
            "$common[ext=$ext]"
        }
        // Try preferred extensions first, then any progressive ≤ maxHeight
        return "$biased/$common"
    }

    /**
     * Returns the direct media URL for a given format selector, without downloading.
     * Uses: yt-dlp -f "<selector>" -g --no-playlist URL
     *
     * @return Result.success(url) if a single progressive URL is found, otherwise failure with reason.
     */
    fun getDirectUrlForFormat(
        url: String,
        formatSelector: String,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

        // Fast network preflight
        val net = checkNetwork(url)
        if (net.isFailure) return Result.failure(IllegalStateException("Network preflight failed: ${net.exceptionOrNull()?.message}"))

        val cmd = buildList {
            add(ytDlpPath)
            addAll(listOf("-f", formatSelector, "-g", "--no-playlist", "--newline"))
            if (noCheckCertificate) add("--no-check-certificate")
            // Respect explicit ffmpeg location if set (won't be used here unless ytdlp needs it for probes)
            ffmpegPath?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
            add(url)
        }

        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        val process = try {
            pb.start()
        } catch (t: Throwable) {
            return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
        }

        // Guard against hanging
        val finished = process.waitFor(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
        if (!finished) {
            process.destroy()
            return Result.failure(IllegalStateException("yt-dlp -g timed out after ${timeoutSec}s"))
        }

        val out = process.inputStream.bufferedReader(Charsets.UTF_8).readLines().map { it.trim() }.filter { it.isNotBlank() }
        val exit = process.exitValue()
        if (exit != 0) {
            val errPreview = out.takeLast(10).joinToString("\n")
            return Result.failure(IllegalStateException("yt-dlp -g failed (exit $exit)\n$errPreview"))
        }

        // For progressive formats, yt-dlp -g should output a single URL.
        // If multiple lines appear, it's likely a split A/V selection — reject to prevent merges.
        return when (out.size) {
            0 -> Result.failure(IllegalStateException("No URL returned by yt-dlp for selector: $formatSelector"))
            1 -> Result.success(out.first())
            else -> Result.failure(IllegalStateException("Multiple URLs returned (likely split A/V). Refusing to avoid merging.\n${
                out.joinToString("\n")
            }"))
        }
    }

    /**
     * Convenience: Get a "medium" quality (<= 480p by default) progressive (audio+video) direct URL.
     * No download, no conversion.
     *
     * @param maxHeight Upper bound for height (default 480 for "medium").
     * @param preferredExts Extension preference order (default mp4, then webm).
     */
    fun getMediumQualityUrl(
        url: String,
        maxHeight: Int = 480,
        preferredExts: List<String> = listOf("mp4", "webm"),
        noCheckCertificate: Boolean = false
    ): Result<String> {
        val selector = progressiveMediumSelector(maxHeight, preferredExts)
        return getDirectUrlForFormat(
            url = url,
            formatSelector = selector,
            noCheckCertificate = noCheckCertificate
        )
    }


    // ====== Command construction & parsing ======

    private fun buildCommand(url: String, options: Options): List<String> {
        val cmd = mutableListOf(ytDlpPath, "--newline")

        // Explicit FFmpeg override if available
        ffmpegPath?.takeIf { it.isNotBlank() }?.let { cmd.addAll(listOf("--ffmpeg-location", it)) }

        // no-check-certificate
        if (options.noCheckCertificate) cmd.add("--no-check-certificate")

        // output directory / template
        downloadDir?.let { dir ->
            if (!dir.exists()) dir.mkdirs()
            val tpl = options.outputTemplate ?: "%(title)s.%(ext)s"
            cmd.addAll(listOf("-o", File(dir, tpl).absolutePath))
        } ?: run {
            options.outputTemplate?.let { tpl -> cmd.addAll(listOf("-o", tpl)) }
        }

        // format selection
        options.format?.let { cmd.addAll(listOf("-f", it)) }

        // user extra args at the end
        if (options.extraArgs.isNotEmpty()) cmd.addAll(options.extraArgs)

        cmd.add(url)
        return cmd
    }

    private fun parseProgress(line: String): Event.Progress? {
        // Typical progress lines: "[download]  12.3% of ...", "[Merger] Merging formats: ...", etc.
        val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
        val match = percentRegex.find(line)
        val pct = match?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        return if (pct != null) Event.Progress(pct, line) else null
    }

    // ====== Failure diagnosis ======

    /** Try to spot a common cause based on output lines. */
    private fun diagnose(lines: List<String>): String? {
        val joined = lines.joinToString("\n").lowercase()

        fun has(vararg needles: String) = needles.any { joined.contains(it.lowercase()) }

        return when {
            has("connection refused", "no route to host", "network is unreachable") ->
                "Connection problem to remote host."
            has("timed out", "timeout", "operation timed out", "read timed out") ->
                "Network timeout."
            has("unknown host", "name or service not known", "temporary failure in name resolution") ->
                "DNS resolution failed."
            has("ssl: certificate verify failed", "self signed certificate", "certificate has expired") ->
                "TLS/Certificate problem (try --no-check-certificate if appropriate)."
            has("http error 403") -> "HTTP 403 Forbidden (access denied)."
            has("http error 429", "too many requests", "rate limited") -> "Rate limited (HTTP 429)."
            has("copyright", "unavailable", "this video is not available") ->
                "Content not available or restricted."
            has("proxy", "socks", "http proxy") ->
                "Proxy/network configuration error."
            else -> null
        }
    }

    // ====== small utils ======

    private fun downloadFile(url: String, dest: File) {
        dest.parentFile?.mkdirs()
        java.net.URI.create(url).toURL().openStream().use { input ->
            Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun makeExecutable(file: File) {
        try {
            val path = file.toPath()
            val perms = Files.getPosixFilePermissions(path).toMutableSet()
            perms.add(PosixFilePermission.OWNER_EXECUTE)
            perms.add(PosixFilePermission.GROUP_EXECUTE)
            perms.add(PosixFilePermission.OTHERS_EXECUTE)
            Files.setPosixFilePermissions(path, perms)
        } catch (_: UnsupportedOperationException) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
        }
    }
}
