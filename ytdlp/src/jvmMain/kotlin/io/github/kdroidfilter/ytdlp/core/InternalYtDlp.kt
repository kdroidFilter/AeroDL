package io.github.kdroidfilter.ytdlp.core

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.util.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

internal class InternalYtDlp(
    private val ytDlpPathProvider: () -> String,
    private val ytDlpPathSetter: (String) -> Unit,
    private val ffmpegPathProvider: () -> String?,
    private val ffmpegPathSetter: (String?) -> Unit,
    private val downloadDirProvider: () -> java.io.File?
) {
    private val ytdlpFetcher = GitHubReleaseFetcher(owner = "yt-dlp", repo = "yt-dlp")

    // -------- Availability / version / update ----------
    fun version(): String? = try {
        val proc = ProcessBuilder(listOf(ytDlpPathProvider(), "--version"))
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val code = proc.waitFor()
        if (code == 0 && out.isNotBlank()) out else null
    } catch (_: Exception) { null }

    fun isAvailable(): Boolean {
        val file = java.io.File(ytDlpPathProvider())
        return file.exists() && file.canExecute() && version() != null
    }

    suspend fun hasUpdate(): Boolean {
        val currentVersion = version() ?: return true
        val latestVersion = ytdlpFetcher.getLatestRelease()?.tag_name ?: return false
        val current = currentVersion.removePrefix("v").trim()
        val latest = latestVersion.removePrefix("v").trim()
        return current != latest
    }

    suspend fun downloadOrUpdate(): Boolean {
        val assetName = PlatformUtils.getYtDlpAssetNameForSystem()
        val destFile = java.io.File(PlatformUtils.getDefaultBinaryPath())
        destFile.parentFile?.mkdirs()

        return try {
            val release = ytdlpFetcher.getLatestRelease() ?: return false
            val asset = release.assets.find { it.name == assetName } ?: return false

            println("Downloading yt-dlp: ${asset.name} (${release.tag_name})")
            PlatformUtils.downloadFile(asset.browser_download_url, destFile)

            if (getOperatingSystem() != OperatingSystem.WINDOWS) PlatformUtils.makeExecutable(destFile)

            if (isAvailable()) {
                println("yt-dlp ready at ${destFile.absolutePath}")
                ytDlpPathSetter(destFile.absolutePath)
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

    // -------- FFmpeg ----------
    suspend fun ensureFfmpegAvailable(forceDownload: Boolean): Boolean {
        ffmpegPathProvider()?.let { if (PlatformUtils.ffmpegVersion(it) != null && !forceDownload) return true }
        PlatformUtils.findFfmpegInSystemPath()?.let { ffmpegPathSetter(it); return true }
        return when (getOperatingSystem()) {
            OperatingSystem.WINDOWS, OperatingSystem.LINUX -> downloadFfmpeg(forceDownload)
            OperatingSystem.MACOS -> false
            else -> false
        }
    }

    suspend fun downloadFfmpeg(forceDownload: Boolean): Boolean {
        val asset = PlatformUtils.getFfmpegAssetNameForSystem() ?: return false
        val result = PlatformUtils.downloadAndInstallFfmpeg(asset, forceDownload)
        if (result != null) ffmpegPathSetter(result)
        return result != null
    }

    // -------- Network ----------
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int, readTimeoutMs: Int): Result<Unit> =
        NetAndArchive.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // -------- Direct URL (no download) ----------
    fun getDirectUrlForFormat(
        url: String,
        formatSelector: String,
        noCheckCertificate: Boolean,
        timeoutSec: Long
    ): Result<String> {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

        val net = checkNetwork(url, 5000, 5000)
        if (net.isFailure) return Result.failure(IllegalStateException("Network preflight failed: ${net.exceptionOrNull()?.message}"))

        val cmd = buildList {
            add(ytDlpPathProvider())
            addAll(listOf("-f", formatSelector, "-g", "--no-playlist", "--newline"))
            if (noCheckCertificate) add("--no-check-certificate")
            ffmpegPathProvider()?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
            add(url)
        }

        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        val process = try { pb.start() } catch (t: Throwable) {
            return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
        }

        val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (!finished) {
            process.destroy()
            return Result.failure(IllegalStateException("yt-dlp -g timed out after ${timeoutSec}s"))
        }

        val out = process.inputStream.bufferedReader(Charsets.UTF_8)
            .readLines().map { it.trim() }.filter { it.isNotBlank() }
        val exit = process.exitValue()
        if (exit != 0) {
            val errPreview = out.takeLast(10).joinToString("\n")
            return Result.failure(IllegalStateException("yt-dlp -g failed (exit $exit)\n$errPreview"))
        }
        return when (out.size) {
            0 -> Result.failure(IllegalStateException("No URL returned by yt-dlp for selector: $formatSelector"))
            1 -> Result.success(out.first())
            else -> Result.failure(IllegalStateException("Multiple URLs returned (likely split A/V). Refusing to avoid merging.\n${out.joinToString("\n")}"))
        }
    }

    fun getMediumQualityUrl(
        url: String,
        maxHeight: Int,
        preferredExts: List<String>,
        noCheckCertificate: Boolean
    ): Result<String> {
        val selector = NetAndArchive.progressiveMediumSelector(maxHeight, preferredExts)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec = 20)
    }

    // -------- Download with streaming events ----------
    fun download(
        url: String,
        options: Options,
        onEvent: (Event) -> Unit
    ): Handle {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

        val net = checkNetwork(url, 5000, 5000)
        if (net.isFailure) {
            onEvent(Event.NetworkProblem(net.exceptionOrNull()?.message ?: "Network not available"))
            onEvent(Event.Error("Network preflight failed."))
            val proc = NetAndArchive.startNoopProcess()
            return Handle(proc, AtomicBoolean(true))
        }

        val cmd = NetAndArchive.buildCommand(
            ytDlpPath = ytDlpPathProvider(),
            ffmpegPath = ffmpegPathProvider(),
            url = url,
            options = options,
            downloadDir = downloadDirProvider()
        )

        val pb = ProcessBuilder(cmd)
            .directory(downloadDirProvider())
            .redirectErrorStream(true)

        val process = try { pb.start() } catch (t: Throwable) {
            onEvent(Event.Error("Failed to start yt-dlp process (permissions or path issue).", t))
            val fake = NetAndArchive.startNoopProcess()
            return Handle(fake, AtomicBoolean(true))
        }

        val cancelled = AtomicBoolean(false)
        val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))

        // Tail for diagnostics
        val tailCapacity = 120
        val tail = ArrayBlockingQueue<String>(tailCapacity)
        fun offerTail(line: String) {
            if (!tail.offer(line)) { tail.poll(); tail.offer(line) }
        }

        onEvent(Event.Started)

        // Reader thread
        Thread({
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    offerTail(l)
                    val progress = NetAndArchive.parseProgress(l)
                    if (progress != null) onEvent(Event.Progress(progress, l)) else onEvent(Event.Log(l))
                }
            } catch (t: Throwable) {
                if (!cancelled.get()) onEvent(Event.Error("I/O error while reading yt-dlp output", t))
            } finally {
                try { reader.close() } catch (_: Exception) {}
            }
        }, "yt-dlp-reader").apply { isDaemon = true }.start()

        // Timeout watchdog
        options.timeout?.let { limit ->
            Thread({
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
            }, "yt-dlp-timeout").apply { isDaemon = true }.start()
        }

        // Completion watcher
        Thread({
            val exit = try { process.waitFor() } catch (_: InterruptedException) { -1 }
            val ok = (exit == 0)
            if (cancelled.get()) {
                onEvent(Event.Cancelled)
            } else if (!ok) {
                val lines = mutableListOf<String>()
                tail.drainTo(lines)
                val diagnostic = NetAndArchive.diagnose(lines)
                val tailPreview = if (lines.isEmpty()) "(no output captured)"
                else lines.takeLast(min(15, lines.size)).joinToString("\n")

                onEvent(Event.Error("yt-dlp failed (exit $exit). ${diagnostic ?: ""}".trim() +
                    "\n--- Last output ---\n$tailPreview"))
            }
            onEvent(Event.Completed(exit, ok))
        }, "yt-dlp-completion").apply { isDaemon = true }.start()

        return Handle(process, cancelled)
    }
}
