package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.core.*
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlp.util.NetAndArchive
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * Main class for interacting with the yt-dlp tool.
 * Manages the binary, FFmpeg, downloads, and metadata extraction.
 */
class YtDlpWrapper {

    // --- User Configuration (modifiable externally) ---
    var ytDlpPath: String = PlatformUtils.getDefaultBinaryPath()
    var ffmpegPath: String? = null
    var downloadDir: File? = File(System.getProperty("user.home"), "Downloads/yt-dlp")

    /**
     * If true, adds the '--no-check-certificate' argument to all yt-dlp commands.
     * This can be overridden on a per-call basis in method invocations.
     */
    var noCheckCertificate: Boolean = false

    private val ytdlpFetcher = GitHubReleaseFetcher(owner = "yt-dlp", repo = "yt-dlp")
    private data class ProcessResult(val exitCode: Int, val stdout: List<String>, val stderr: String)

    // --- Initialization Events for UI ---
    sealed interface InitEvent {
        data object CheckingYtDlp : InitEvent
        data object DownloadingYtDlp : InitEvent
        data object UpdatingYtDlp : InitEvent
        data object EnsuringFfmpeg : InitEvent
        data class YtDlpProgress(val bytesRead: Long, val totalBytes: Long?, val percent: Double?) : InitEvent
        data class FfmpegProgress(val bytesRead: Long, val totalBytes: Long?, val percent: Double?) : InitEvent
        data class Completed(val success: Boolean) : InitEvent
        data class Error(val message: String, val cause: Throwable? = null) : InitEvent
    }

    /**
     * Launches the initialization process in a provided CoroutineScope.
     * Use onEvent to observe the progress.
     */
    fun initializeIn(scope: CoroutineScope, onEvent: (InitEvent) -> Unit = {}): Job =
        scope.launch { initialize(onEvent) }

    /**
     * Performs initialization (checks/downloads yt-dlp and FFmpeg).
     * Runs asynchronously and emits progress events.
     */
    suspend fun initialize(onEvent: (InitEvent) -> Unit = {}): Boolean {
        fun pct(read: Long, total: Long?): Double? = total?.takeIf { it > 0 }?.let { read * 100.0 / it }
        try {
            onEvent(InitEvent.CheckingYtDlp)
            if (!isAvailable()) {
                onEvent(InitEvent.DownloadingYtDlp)
                if (!downloadOrUpdate { r, t -> onEvent(InitEvent.YtDlpProgress(r, t, pct(r, t))) }) {
                    onEvent(InitEvent.Error("Could not download yt-dlp", null))
                    onEvent(InitEvent.Completed(false)); return false
                }
            } else if (hasUpdate()) {
                onEvent(InitEvent.UpdatingYtDlp)
                downloadOrUpdate { r, t -> onEvent(InitEvent.YtDlpProgress(r, t, pct(r, t))) }
            }

            onEvent(InitEvent.EnsuringFfmpeg)
            ensureFfmpegAvailable(false) { r, t -> onEvent(InitEvent.FfmpegProgress(r, t, pct(r, t))) }

            val success = isAvailable()
            onEvent(InitEvent.Completed(success))
            return success
        } catch (t: Throwable) {
            onEvent(InitEvent.Error(t.message ?: "Initialization error", t))
            onEvent(InitEvent.Completed(false))
            return false
        }
    }

    // --- Availability / Version / Update ---
    fun version(): String? = try {
        val proc = ProcessBuilder(listOf(ytDlpPath, "--version"))
            .redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        if (proc.waitFor() == 0 && out.isNotBlank()) out else null
    } catch (_: Exception) { null }

    fun isAvailable(): Boolean {
        val file = File(ytDlpPath)
        return file.exists() && file.canExecute() && version() != null
    }

    suspend fun hasUpdate(): Boolean {
        val currentVersion = version() ?: return true
        val latestVersion = ytdlpFetcher.getLatestRelease()?.tag_name ?: return false
        return currentVersion.removePrefix("v").trim() != latestVersion.removePrefix("v").trim()
    }

    suspend fun downloadOrUpdate(onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        val assetName = PlatformUtils.getYtDlpAssetNameForSystem()
        val destFile = File(PlatformUtils.getDefaultBinaryPath())
        destFile.parentFile?.mkdirs()

        return try {
            val release = ytdlpFetcher.getLatestRelease() ?: return false
            val asset = release.assets.find { it.name == assetName } ?: return false

            PlatformUtils.downloadFile(asset.browser_download_url, destFile, onProgress)
            if (getOperatingSystem() != OperatingSystem.WINDOWS) PlatformUtils.makeExecutable(destFile)

            if (isAvailable()) {
                ytDlpPath = destFile.absolutePath
                true
            } else {
                destFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- FFmpeg ---
    fun getDefaultFfmpegPath(): String = PlatformUtils.getDefaultFfmpegPath()

    fun ensureFfmpegAvailable(forceDownload: Boolean = false, onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        ffmpegPath?.let {
            if (PlatformUtils.ffmpegVersion(it) != null && !forceDownload) return true
        }
        PlatformUtils.findFfmpegInSystemPath()?.let {
            ffmpegPath = it
            return true
        }
        if (getOperatingSystem() in listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX)) {
            val asset = PlatformUtils.getFfmpegAssetNameForSystem() ?: return false
            val result = PlatformUtils.downloadAndInstallFfmpeg(asset, forceDownload, onProgress)
            if (result != null) ffmpegPath = result
            return result != null
        }
        return false
    }

    // --- Network Pre-check ---
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000): Result<Unit> =
        NetAndArchive.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // --- Downloader ---
    fun download(url: String, options: Options = Options(), onEvent: (Event) -> Unit): Handle {
        require(isAvailable()) { "yt-dlp is not available." }

        checkNetwork(url, 5000, 5000).getOrElse {
            onEvent(Event.NetworkProblem(it.message ?: "Network unavailable"))
            onEvent(Event.Error("Network pre-check failed."))
            return Handle(NetAndArchive.startNoopProcess(), AtomicBoolean(true))
        }

        // Apply global noCheckCertificate if not specified in options
        val finalOptions = if (options.noCheckCertificate) options else options.copy(noCheckCertificate = this.noCheckCertificate)
        val cmd = NetAndArchive.buildCommand(ytDlpPath, ffmpegPath, url, finalOptions, downloadDir)
        val process = try {
            ProcessBuilder(cmd).directory(downloadDir).redirectErrorStream(true).start()
        } catch (t: Throwable) {
            onEvent(Event.Error("Failed to start the yt-dlp process.", t))
            return Handle(NetAndArchive.startNoopProcess(), AtomicBoolean(true))
        }

        val cancelled = AtomicBoolean(false)
        val tail = ArrayBlockingQueue<String>(120)
        onEvent(Event.Started)

        // Reader thread
        Thread({
            try {
                process.inputStream.bufferedReader(StandardCharsets.UTF_8).forEachLine { line ->
                    if (!tail.offer(line)) { tail.poll(); tail.offer(line) }
                    val progress = NetAndArchive.parseProgress(line)
                    if (progress != null) onEvent(Event.Progress(progress, line)) else onEvent(Event.Log(line))
                }
            } catch (t: Throwable) {
                if (!cancelled.get()) onEvent(Event.Error("I/O error while reading yt-dlp output", t))
            }
        }, "yt-dlp-reader").apply { isDaemon = true }.start()

        // Watchdog threads (timeout and completion)
        startWatchdogThreads(process, finalOptions, cancelled, tail, onEvent)
        return Handle(process, cancelled)
    }

    // --- Direct URL Helpers ---
    fun getDirectUrlForFormat(url: String, formatSelector: String, noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> {
        require(isAvailable()) { "yt-dlp is not available." }
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val args = buildList {
            addAll(listOf("-f", formatSelector, "-g", "--no-playlist", "--newline"))
            if (useNoCheckCert) add("--no-check-certificate")
            add(url)
        }

        val result = executeCommand(args, timeoutSec).getOrElse { return Result.failure(it) }

        if (result.exitCode != 0) {
            val errorDetails = if (result.stderr.isNotBlank()) result.stderr else result.stdout.joinToString("\n")
            return Result.failure(IllegalStateException("yt-dlp -g failed (exit ${result.exitCode})\n$errorDetails"))
        }

        val out = result.stdout.map { it.trim() }.filter { it.isNotBlank() }
        return when {
            out.isEmpty() -> Result.failure(IllegalStateException("No URL returned for selector: $formatSelector"))
            out.size == 1 -> Result.success(out.first())
            else -> Result.failure(IllegalStateException("Multiple URLs returned (likely separate A/V).\n${out.joinToString("\n")}"))
        }
    }

    // --- Simple Resolution Presets ---
    enum class Preset(val height: Int) { P360(360), P480(480), P720(720), P1080(1080), P1440(1440), P2160(2160) }

    fun getProgressiveUrl(url: String, preset: Preset, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> =
        getProgressiveUrlForHeight(url, preset.height, preferredExts, noCheckCertificate, timeoutSec)

    fun downloadAt(url: String, preset: Preset, outputTemplate: String? = null, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle =
        downloadAtHeight(url, preset.height, preferredExts, noCheckCertificate, outputTemplate, extraArgs, timeout, onEvent)

    fun downloadMp4At(url: String, preset: Preset, outputTemplate: String? = "%(title)s.%(ext)s", noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), recodeIfNeeded: Boolean = false, onEvent: (Event) -> Unit): Handle =
        downloadAtHeightMp4(url, preset.height, noCheckCertificate, outputTemplate, extraArgs, timeout, recodeIfNeeded, onEvent)

    // --- Audio Downloads ---
    fun downloadAudioMp3(url: String, outputTemplate: String? = "%(title)s.%(ext)s", audioQuality: Int = 0, noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle {
        val args = listOf("--extract-audio", "--audio-format", "mp3", "--audio-quality", audioQuality.coerceIn(0, 10).toString(), "--add-metadata", "--embed-thumbnail", "-f", "bestaudio/best")
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    enum class AudioQualityPreset(val bitrate: String, val description: String) {
        LOW("96k", "Space saving"), MEDIUM("128k", "Standard quality"), HIGH("192k", "High quality"),
        VERY_HIGH("256k", "Very high quality"), MAXIMUM("320k", "Maximum quality")
    }

    fun downloadAudioMp3WithPreset(url: String, preset: AudioQualityPreset = AudioQualityPreset.HIGH, outputTemplate: String? = "%(title)s.%(ext)s", noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle {
        val postprocessorArg = "ffmpeg:-q:a ${when(preset){
            AudioQualityPreset.LOW -> 5; AudioQualityPreset.MEDIUM -> 4; AudioQualityPreset.HIGH -> 2;
            AudioQualityPreset.VERY_HIGH -> 1; AudioQualityPreset.MAXIMUM -> 0
        }}"
        val args = listOf("--extract-audio", "--audio-format", "mp3", "--add-metadata", "--embed-thumbnail", "-f", "bestaudio/best", "--postprocessor-args", postprocessorArg)
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    fun getAudioStreamUrl(url: String, preferredCodecs: List<String> = listOf("opus", "m4a", "mp3", "aac", "vorbis"), noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> {
        val codecSelectors = preferredCodecs.joinToString("/") { "bestaudio[acodec=$it]" }
        val formatSelector = "$codecSelectors/bestaudio/best"
        return getDirectUrlForFormat(url, formatSelector, noCheckCertificate, timeoutSec)
    }

    // --- Metadata Extraction ---
    fun getVideoInfo(url: String, extractFlat: Boolean = false, noCheckCertificate: Boolean = false, timeoutSec: Long = 20, maxHeight: Int = 1080, preferredExts: List<String> = listOf("mp4", "webm")): Result<VideoInfo> {
        val args = buildList {
            add("--no-playlist")
            if (extractFlat) add("--flat-playlist")
        }
        return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json ->
            parseVideoInfoFromJson(json, maxHeight, preferredExts)
        }
    }

    fun getPlaylistInfo(url: String, extractFlat: Boolean = true, noCheckCertificate: Boolean = false, timeoutSec: Long = 60): Result<PlaylistInfo> {
        val args = if (extractFlat) listOf("--flat-playlist") else emptyList()
        return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json ->
            parsePlaylistInfoFromJson(json)
        }
    }

    fun getVideoInfoList(url: String, maxEntries: Int? = null, extractFlat: Boolean = true, noCheckCertificate: Boolean = false, timeoutSec: Long = 60, maxHeight: Int = 1080, preferredExts: List<String> = listOf("mp4", "webm")): Result<List<VideoInfo>> {
        val args = buildList {
            if (extractFlat) add("--flat-playlist")
            maxEntries?.let { addAll(listOf("--playlist-end", it.toString())) }
        }
        return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { jsonString ->
            val jsonLines = jsonString.lines().filter { it.isNotBlank() }
            if (jsonLines.size == 1 && jsonLines[0].trim().startsWith("{\"_type\":\"playlist\"")) {
                parsePlaylistInfoFromJson(jsonLines[0]).entries
            } else {
                jsonLines.map { line -> parseVideoInfoFromJson(line, maxHeight, preferredExts) }
            }
        }
    }

    // =================================================================
    // =================== INTERNAL PRIVATE METHODS ===================
    // =================================================================

    private fun getProgressiveUrlForHeight(url: String, height: Int, preferredExts: List<String>, noCheckCertificate: Boolean, timeoutSec: Long): Result<String> {
        val selector = NetAndArchive.selectorProgressiveExact(height, preferredExts)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec)
    }

    private fun downloadAtHeight(url: String, height: Int, preferredExts: List<String>, noCheckCertificate: Boolean, outputTemplate: String?, extraArgs: List<String>, timeout: Duration?, onEvent: (Event) -> Unit): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val opts = Options(format = NetAndArchive.selectorDownloadExact(height, preferredExts), outputTemplate = outputTemplate, noCheckCertificate = useNoCheckCert, extraArgs = extraArgs, timeout = timeout)
        return download(url, opts, onEvent)
    }

    private fun downloadAtHeightMp4(url: String, height: Int, noCheckCertificate: Boolean, outputTemplate: String?, extraArgs: List<String>, timeout: Duration?, recodeIfNeeded: Boolean, onEvent: (Event) -> Unit): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val opts = Options(format = NetAndArchive.selectorDownloadExactMp4(height), outputTemplate = outputTemplate, noCheckCertificate = useNoCheckCert, extraArgs = extraArgs, timeout = timeout, targetContainer = "mp4", allowRecode = recodeIfNeeded)
        return download(url, opts, onEvent)
    }

    private fun downloadAudioInternal(url: String, outputTemplate: String?, noCheckCertificate: Boolean, extraArgs: List<String>, timeout: Duration?, audioArgs: List<String>, onEvent: (Event) -> Unit): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val options = Options(outputTemplate = outputTemplate, noCheckCertificate = useNoCheckCert, extraArgs = audioArgs + extraArgs, timeout = timeout)
        return download(url, options, onEvent)
    }

    private fun startWatchdogThreads(process: Process, options: Options, cancelled: AtomicBoolean, tail: ArrayBlockingQueue<String>, onEvent: (Event) -> Unit) {
        options.timeout?.let { limit ->
            Thread({
                try {
                    if (!process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS) && !cancelled.getAndSet(true)) {
                        process.destroy()
                        onEvent(Event.Error("Download timed out after ${limit.toMinutes()} minutes."))
                    }
                } catch (_: InterruptedException) {}
            }, "yt-dlp-timeout").apply { isDaemon = true }.start()
        }

        Thread({
            val exit = try { process.waitFor() } catch (_: InterruptedException) { -1 }
            if (cancelled.get()) {
                onEvent(Event.Cancelled)
            } else if (exit != 0) {
                val lines = mutableListOf<String>().also { tail.drainTo(it) }
                val diagnostic = NetAndArchive.diagnose(lines)
                val tailPreview = if (lines.isEmpty()) "(no output captured)" else lines.takeLast(min(15, lines.size)).joinToString("\n")
                onEvent(Event.Error("yt-dlp failed (exit $exit). ${diagnostic ?: ""}".trim() + "\n--- Last output ---\n$tailPreview"))
            }
            onEvent(Event.Completed(exit, exit == 0))
        }, "yt-dlp-completion").apply { isDaemon = true }.start()
    }

    private fun executeCommand(args: List<String>, timeoutSec: Long): Result<ProcessResult> {
        val cmd = buildList {
            add(ytDlpPath)
            addAll(args)
            ffmpegPath?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
        }

        val process = try { ProcessBuilder(cmd).start() }
        catch (t: Throwable) { return Result.failure(IllegalStateException("Failed to start yt-dlp process", t)) }

        val stdoutLines = mutableListOf<String>()
        val stderr = StringBuilder()
        val outReader = Thread { process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.forEachLine(stdoutLines::add) } }
        val errReader = Thread { process.errorStream.bufferedReader(StandardCharsets.UTF_8).use { reader -> reader.forEachLine { stderr.appendLine(it) } } }
        outReader.start(); errReader.start()

        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            outReader.interrupt(); errReader.interrupt()
            return Result.failure(IllegalStateException("yt-dlp command timed out after ${timeoutSec}s"))
        }

        outReader.join(2000); errReader.join(2000)
        return Result.success(ProcessResult(process.exitValue(), stdoutLines, stderr.toString()))
    }

    private fun extractMetadata(url: String, noCheckCertificate: Boolean, timeoutSec: Long, extraArgs: List<String>): Result<String> {
        require(isAvailable()) { "yt-dlp is not available." }
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val cmdArgs = buildList {
            add("--dump-json"); add("--no-warnings")
            if (useNoCheckCert) add("--no-check-certificate")
            addAll(extraArgs); add(url)
        }
        val pb = ProcessBuilder(buildList { add(ytDlpPath); addAll(cmdArgs) }).redirectErrorStream(false)
        val process = try { pb.start() }
        catch (t: Throwable) { return Result.failure(IllegalStateException("Failed to start yt-dlp process", t)) }

        val jsonOutput = StringBuilder()
        val errorOutput = StringBuilder()
        val outputReader = Thread { process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { it.forEach(jsonOutput::appendLine) } }
        val errorReader = Thread { process.errorStream.bufferedReader(StandardCharsets.UTF_8).useLines { it.forEach(errorOutput::appendLine) } }
        outputReader.start(); errorReader.start()

        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return Result.failure(IllegalStateException("Metadata extraction timed out after ${timeoutSec}s"))
        }
        outputReader.join(5000); errorReader.join(5000)

        return if (process.exitValue() == 0) Result.success(jsonOutput.toString())
        else Result.failure(IllegalStateException("Metadata extraction failed (exit ${process.exitValue()})\n${errorOutput}"))
    }
}