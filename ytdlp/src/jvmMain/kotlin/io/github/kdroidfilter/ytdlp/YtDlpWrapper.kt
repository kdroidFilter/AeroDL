package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.core.*
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlp.util.NetAndArchive
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.debugln
import kotlinx.coroutines.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Main class for interacting with the yt-dlp tool.
 * All caches are process-lifetime (no TTL) and are only invalidated when the binary
 * path changes or an explicit download/update is performed.
 */
class YtDlpWrapper {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- Lifetime caches (thread-safe) ---

    @Volatile
    private var cachedVersion: String? = null

    @Volatile
    private var cachedLatestReleaseTag: String? = null

    // Bounded lifetime metadata cache (no TTL, LRU capped by size to protect memory)
    private class LruMap<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }
    private val metadataCacheCapacity = 512
    private val metadataCacheLock = Any()
    private val metadataCache = LruMap<String, MetaCacheEntry>(metadataCacheCapacity)

    private data class MetaCacheEntry(val json: String)

    // --- User Configuration (modifiable externally) ---
    var ytDlpPath: String = PlatformUtils.getDefaultBinaryPath()
        set(value) {
            field = value
            // Invalidate all caches when path changes
            cachedVersion = null
            cachedLatestReleaseTag = null
            synchronized(metadataCacheLock) { metadataCache.clear() }
        }

    var ffmpegPath: String? = null
    var downloadDir: File? = File(System.getProperty("user.home"), "Downloads/yt-dlp")

    /**
     * If true, adds the '--no-check-certificate' argument to all yt-dlp commands.
     * This can be overridden on a per-call basis in method invocations.
     */
    var noCheckCertificate: Boolean = false

    /**
     * If set, adds '--cookies-from-browser <browser>' to commands. Example: "firefox".
     * This can be overridden per call via Options.cookiesFromBrowser.
     */
    var cookiesFromBrowser: String? = null

    /**
     * Controls whether to embed the video thumbnail into MP3 audio downloads.
     * When true, yt-dlp is invoked with "--embed-thumbnail" for MP3 downloads.
     */
    var embedThumbnailInMp3: Boolean = true

    /**
     * Controls whether to remove sponsored segments from downloaded videos.
     * When true, yt-dlp is invoked with "--sponsorblock-remove default".
     */
    var sponsorBlockRemove: Boolean = false

    private val httpClient = KtorConfig.createHttpClient()
    private val ytdlpFetcher = GitHubReleaseFetcher(owner = "yt-dlp", repo = "yt-dlp", httpClient = httpClient)
    private val ffmpegFetcher = GitHubReleaseFetcher(owner = "yt-dlp", repo = "FFmpeg-Builds", httpClient = httpClient)
    private val ffmpegMacOsFetcher = GitHubReleaseFetcher(owner = "eugeneware", repo = "ffmpeg-static", httpClient = httpClient)

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

    // --- Public initialization helpers ---

    fun initializeIn(scope: CoroutineScope, onEvent: (InitEvent) -> Unit = {}): Job =
        scope.launch { initialize(onEvent) }

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

    /**
     * Lifetime-cached version. First resolve by spawning the process once, then reuse.
     */
    suspend fun version(): String? = withContext(Dispatchers.IO) {
        cachedVersion?.let { return@withContext it }
        try {
            val proc = ProcessBuilder(listOf(ytDlpPath, "--version")).redirectErrorStream(true).start()
            val exited = withTimeoutOrNull(2_000) { proc.waitFor() }
            if (exited == null) {
                proc.destroyForcibly()
                return@withContext null
            }
            val out = proc.inputStream.bufferedReader().readText().trim()
            val res = if (proc.exitValue() == 0 && out.isNotBlank()) out else null
            if (res != null) cachedVersion = res
            res
        } catch (_: Exception) {
            null
        }
    }

    suspend fun ffmpegVersion(): String? {
        val path = ffmpegPath ?: return null
        return PlatformUtils.ffmpegVersion(path)
    }

    suspend fun isAvailable(): Boolean {
        val file = File(ytDlpPath)
        if (!(file.exists() && file.canExecute())) return false
        // Avoid respawning if version is already known
        return version() != null
    }

    /**
     * Determine if a newer release exists on GitHub.
     * The "latest tag" is cached for the entire process lifetime to avoid repeated network calls.
     * Cache is invalidated after successful download/update or when ytDlpPath changes.
     */
    suspend fun hasUpdate(): Boolean {
        val currentVersion = version() ?: return true
        val latestTag = cachedLatestReleaseTag ?: ytdlpFetcher.getLatestRelease()?.tag_name?.also {
            cachedLatestReleaseTag = it
        } ?: return false
        return currentVersion.removePrefix("v").trim() != latestTag.removePrefix("v").trim()
    }

    /**
     * Download or update to the latest available asset for this platform.
     * Invalidates caches after successful install.
     */
    suspend fun downloadOrUpdate(onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        val assetName = PlatformUtils.getYtDlpAssetNameForSystem()
        val destFile = File(PlatformUtils.getDefaultBinaryPath())
        destFile.parentFile?.mkdirs()

        return try {
            val release = ytdlpFetcher.getLatestRelease() ?: return false
            val asset = release.assets.find { it.name == assetName } ?: return false

            PlatformUtils.downloadFile(asset.browser_download_url, destFile, onProgress)
            if (getOperatingSystem() != OperatingSystem.WINDOWS) PlatformUtils.makeExecutable(destFile)

            if (destFile.exists() && destFile.canExecute()) {
                // Reset caches so subsequent calls re-resolve version and latest tag once.
                ytDlpPath = destFile.absolutePath
                cachedVersion = null
                cachedLatestReleaseTag = null
                synchronized(metadataCacheLock) { metadataCache.clear() }
                // Verify availability once; cache will be filled by version()
                isAvailable()
            } else {
                destFile.delete()
                false
            }
        } catch (e: Exception) {
            errorln { "Error during yt-dlp download/update: ${e.stackTraceToString()}" }
            false
        }
    }

    // --- FFmpeg ---

    fun getDefaultFfmpegPath(): String = PlatformUtils.getDefaultFfmpegPath()

    suspend fun ensureFfmpegAvailable(
        forceDownload: Boolean = false,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean {
        ffmpegPath?.let {
            if (PlatformUtils.ffmpegVersion(it) != null && !forceDownload) return true
        }
        PlatformUtils.findFfmpegInSystemPath()?.let {
            ffmpegPath = it
            return true
        }
        if (getOperatingSystem() in listOf(OperatingSystem.WINDOWS, OperatingSystem.LINUX, OperatingSystem.MACOS)) {
            val assetPattern = PlatformUtils.getFfmpegAssetPatternForSystem() ?: return false
            val fetcher = if (getOperatingSystem() == OperatingSystem.MACOS) ffmpegMacOsFetcher else ffmpegFetcher
            val result = PlatformUtils.downloadAndInstallFfmpeg(assetPattern, forceDownload, fetcher, onProgress)
            if (result != null) ffmpegPath = result
            return result != null
        }
        return false
    }

    // --- Network Pre-check ---

    fun checkNetwork(
        targetUrl: String,
        connectTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 5000
    ): Result<Unit> =
        NetAndArchive.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // --- Downloader ---

    fun download(url: String, options: Options = Options(), onEvent: (Event) -> Unit): Handle {
        val job = scope.launch {
            infoln { "[YtDlpWrapper] Starting download for URL: $url" }
            if (!isAvailable()) {
                val error = "yt-dlp is not available. Please call initialize() first."
                errorln { "[YtDlpWrapper] $error" }
                onEvent(Event.Error(error))
                return@launch
            }

            infoln { "[YtDlpWrapper] Checking network connectivity..." }
            checkNetwork(url, 5000, 5000).getOrElse {
                val networkError = it.message ?: "Network unavailable"
                errorln { "[YtDlpWrapper] Network pre-check failed: $networkError" }
                onEvent(Event.NetworkProblem(networkError))
                onEvent(Event.Error("Network pre-check failed."))
                return@launch
            }
            infoln { "[YtDlpWrapper] Network check passed" }

            val finalOptions = options.copy(
                noCheckCertificate = if (options.noCheckCertificate) true else this@YtDlpWrapper.noCheckCertificate,
                cookiesFromBrowser = options.cookiesFromBrowser ?: this@YtDlpWrapper.cookiesFromBrowser
            )

            options.subtitles?.let { subOpts ->
                infoln { "[YtDlpWrapper] Subtitle options provided: languages=${subOpts.languages}, embed=${subOpts.embedSubtitles}, writeAuto=${subOpts.writeAutoSubtitles}" }
            }

            val cmd = NetAndArchive.buildCommand(ytDlpPath, ffmpegPath, url, finalOptions, downloadDir)
            infoln { "[YtDlpWrapper] Built command with ${cmd.size} arguments" }
            debugln { "[YtDlpWrapper] Full command: ${cmd.joinToString(" ")}" }

            val process: Process = try {
                ProcessBuilder(cmd).directory(downloadDir).redirectErrorStream(true).start()
            } catch (t: Throwable) {
                val error = "Failed to start the yt-dlp process: ${t.message}"
                errorln { "[YtDlpWrapper] $error" }
                errorln { "[YtDlpWrapper] Stack trace: ${t.stackTraceToString()}" }
                onEvent(Event.Error(error, t))
                return@launch
            }

            infoln { "[YtDlpWrapper] Process started successfully" }
            onEvent(Event.Started)
            val tail = ArrayBlockingQueue<String>(120)

            try {
                val downloadLogic: suspend CoroutineScope.() -> Unit = {
                    val readerJob = launch {
                        try {
                            process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                                lines.forEach { line ->
                                    if (!isActive) return@forEach
                                    if (!tail.offer(line)) {
                                        tail.poll(); tail.offer(line)
                                    }
                                    val progress = NetAndArchive.parseProgress(line)
                                    if (progress != null) {
                                        val speed = NetAndArchive.parseSpeedBytesPerSec(line)
                                        onEvent(Event.Progress(progress, speed, line))
                                    } else {
                                        onEvent(Event.Log(line))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (isActive) {
                                val error = "I/O error while reading yt-dlp output: ${e.message}"
                                errorln { "[YtDlpWrapper] $error" }
                                errorln { "[YtDlpWrapper] Stack trace: ${e.stackTraceToString()}" }
                                onEvent(Event.Error(error, e))
                            }
                        }
                    }

                    val exitCode = process.waitFor()
                    readerJob.join()

                    if (exitCode != 0) {
                        val lines = mutableListOf<String>().also { tail.drainTo(it) }
                        val diagnostic = NetAndArchive.diagnose(lines)

                        // Extract all ERROR lines
                        val errorLines = lines.filter { it.trim().startsWith("ERROR:", ignoreCase = true) }

                        // Build error message
                        val errorMsg = buildString {
                            append("yt-dlp failed (exit $exitCode). ${diagnostic ?: ""}".trim())

                            if (errorLines.isNotEmpty()) {
                                append("\n\n")
                                append("Errors:\n")
                                errorLines.forEach { errorLine ->
                                    append("• ${errorLine.removePrefix("ERROR:").trim()}\n")
                                }
                            }

                            append("\n--- Last output ---\n")
                            val tailPreview = if (lines.isEmpty()) "(no output captured)" else lines.takeLast(min(15, lines.size)).joinToString("\n")
                            append(tailPreview)
                        }

                        errorln { "[YtDlpWrapper] Download failed with exit code $exitCode" }
                        errorln { "[YtDlpWrapper] Diagnostic: ${diagnostic ?: "none"}" }
                        if (errorLines.isNotEmpty()) {
                            errorln { "[YtDlpWrapper] ERROR lines found: ${errorLines.joinToString("; ")}" }
                        }
                        errorln { "[YtDlpWrapper] Last output:\n${lines.takeLast(min(15, lines.size)).joinToString("\n")}" }
                        onEvent(Event.Error(errorMsg))
                    } else {
                        infoln { "[YtDlpWrapper] Download completed successfully" }
                    }
                    onEvent(Event.Completed(exitCode, exitCode == 0))
                }

                val timeoutMillis = finalOptions.timeout?.toMillis()
                if (timeoutMillis != null) {
                    withTimeoutOrNull(timeoutMillis) {
                        downloadLogic()
                    } ?: run {
                        process.destroyForcibly()
                        onEvent(Event.Error("Download timed out after ${finalOptions.timeout.toMinutes()} minutes."))
                        onEvent(Event.Completed(-1, false))
                    }
                } else {
                    downloadLogic()
                }

            } catch (e: CancellationException) {
                process.destroyForcibly()
                onEvent(Event.Cancelled)
            } catch (t: Throwable) {
                process.destroyForcibly()
                onEvent(Event.Error("An unexpected error occurred during download.", t))
                onEvent(Event.Completed(-1, false))
            }
        }
        return Handle(job)
    }

    // --- Direct URL Helpers ---

    suspend fun getDirectUrlForFormat(
        url: String,
        formatSelector: String,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> {
        if (!isAvailable()) return Result.failure(IllegalStateException("yt-dlp is not available."))
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val args = buildList {
            addAll(listOf("-f", formatSelector, "-g", "--no-playlist", "--newline"))
            if (useNoCheckCert) add("--no-check-certificate")
            cookiesFromBrowser?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--cookies-from-browser", it)) }
            add(url)
        }

        val result = executeCommand(args, timeoutSec).getOrElse { return Result.failure(it) }

        if (result.exitCode != 0) {
            val errorDetails = result.stderr.ifBlank { result.stdout.joinToString("\n") }
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

    enum class Preset(val height: Int) {
        P136(136),   // Common in HLS/m3u8 streams
        P144(144),   // YouTube very low quality
        P226(226),   // Common in HLS/m3u8 streams
        P240(240),   // YouTube low quality
        P270(270),   // Common in some streams
        P340(340),   // Common in HLS/m3u8 streams
        P360(360),   // Standard definition
        P406(406),   // Common in some streams
        P454(454),   // Common in HLS/m3u8 streams
        P480(480),   // Standard definition
        P540(540),   // Common in some streams
        P680(680),   // Common in HLS/m3u8 streams
        P720(720),   // HD
        P810(810),   // Common in some streams
        P1020(1020), // Common in HLS/m3u8 streams
        P1080(1080), // Full HD
        P1360(1360), // Common in HLS/m3u8 streams
        P1440(1440), // 2K
        P2040(2040), // Common in HLS/m3u8 streams
        P2160(2160)  // 4K
    }

    suspend fun getProgressiveUrl(
        url: String,
        preset: Preset,
        preferredExts: List<String> = listOf("mp4", "webm"),
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> =
        getProgressiveUrlForHeight(url, preset.height, preferredExts, noCheckCertificate, timeoutSec)

    fun downloadAt(
        url: String,
        preset: Preset,
        outputTemplate: String? = null,
        preferredExts: List<String> = listOf("mp4", "webm"),
        noCheckCertificate: Boolean = false,
        extraArgs: List<String> = emptyList(),
        timeout: Duration? = Duration.ofMinutes(30),
        subtitles: SubtitleOptions? = null,
        onEvent: (Event) -> Unit
    ): Handle =
        downloadAtHeight(
            url,
            preset.height,
            preferredExts,
            noCheckCertificate,
            outputTemplate,
            extraArgs,
            timeout,
            subtitles,
            onEvent
        )

    fun downloadMp4At(
        url: String,
        preset: Preset,
        outputTemplate: String? = "%(title)s.%(ext)s",
        noCheckCertificate: Boolean = false,
        extraArgs: List<String> = emptyList(),
        timeout: Duration? = Duration.ofMinutes(30),
        recodeIfNeeded: Boolean = false,
        subtitles: SubtitleOptions? = null,
        onEvent: (Event) -> Unit
    ): Handle =
        downloadAtHeightMp4(
            url,
            preset.height,
            noCheckCertificate,
            outputTemplate,
            extraArgs,
            timeout,
            recodeIfNeeded,
            subtitles,
            onEvent
        )

    // --- Audio Downloads ---

    fun downloadAudioMp3(
        url: String,
        outputTemplate: String? = "%(title)s.%(ext)s",
        audioQuality: Int = 0,
        noCheckCertificate: Boolean = false,
        extraArgs: List<String> = emptyList(),
        timeout: Duration? = Duration.ofMinutes(30),
        onEvent: (Event) -> Unit
    ): Handle {
        val args = buildList {
            addAll(listOf(
                "--extract-audio",
                "--audio-format", "mp3",
                "--audio-quality", audioQuality.coerceIn(0, 10).toString(),
                "--add-metadata"
            ))
            if (embedThumbnailInMp3) add("--embed-thumbnail")
            addAll(listOf("-f", "bestaudio/best"))
        }
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    enum class AudioQualityPreset(val bitrate: String, val description: String) {
        LOW("96k", "Space saving"),
        MEDIUM("128k", "Standard quality"),
        HIGH("192k", "High quality"),
        VERY_HIGH("256k", "Very high quality"),
        MAXIMUM("320k", "Maximum quality")
    }

    fun downloadAudioMp3WithPreset(
        url: String,
        preset: AudioQualityPreset = AudioQualityPreset.HIGH,
        outputTemplate: String? = "%(title)s.%(ext)s",
        noCheckCertificate: Boolean = false,
        extraArgs: List<String> = emptyList(),
        timeout: Duration? = Duration.ofMinutes(30),
        onEvent: (Event) -> Unit
    ): Handle {
        val postprocessorArg = "ffmpeg:-q:a " + when (preset) {
            AudioQualityPreset.LOW -> 5
            AudioQualityPreset.MEDIUM -> 4
            AudioQualityPreset.HIGH -> 2
            AudioQualityPreset.VERY_HIGH -> 1
            AudioQualityPreset.MAXIMUM -> 0
        }
        val args = buildList {
            addAll(listOf(
                "--extract-audio",
                "--audio-format", "mp3",
                "--add-metadata"
            ))
            if (embedThumbnailInMp3) add("--embed-thumbnail")
            addAll(listOf(
                "-f", "bestaudio/best",
                "--postprocessor-args", postprocessorArg
            ))
        }
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    suspend fun getAudioStreamUrl(
        url: String,
        preferredCodecs: List<String> = listOf("opus", "m4a", "mp3", "aac", "vorbis"),
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> {
        val codecSelectors = preferredCodecs.joinToString("/") { "bestaudio[acodec=$it]" }
        val formatSelector = "$codecSelectors/bestaudio/best"
        return getDirectUrlForFormat(url, formatSelector, noCheckCertificate, timeoutSec)
    }

    // --- Metadata Extraction (lifetime-cached) ---

    suspend fun getVideoInfo(
        url: String,
        extractFlat: Boolean = false,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 12,
        maxHeight: Int = 1080,
        preferredExts: List<String> = listOf("mp4", "webm")
    ): Result<VideoInfo> {
        val args = buildList {
            add("--no-playlist")
            if (extractFlat) add("--flat-playlist")
        }
        return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json ->
            parseVideoInfoFromJson(json, maxHeight, preferredExts)
        }
    }

    suspend fun getPlaylistInfo(
        url: String,
        extractFlat: Boolean = true,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 60
    ): Result<PlaylistInfo> {
        val args = if (extractFlat) listOf("--flat-playlist") else emptyList()
        return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json ->
            parsePlaylistInfoFromJson(json)
        }
    }

    suspend fun getVideoInfoList(
        url: String,
        maxEntries: Int? = null,
        extractFlat: Boolean = true,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 60,
        maxHeight: Int = 1080,
        preferredExts: List<String> = listOf("mp4", "webm")
    ): Result<List<VideoInfo>> {
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

    private suspend fun getProgressiveUrlForHeight(
        url: String,
        height: Int,
        preferredExts: List<String>,
        noCheckCertificate: Boolean,
        timeoutSec: Long
    ): Result<String> {
        val selector = NetAndArchive.selectorProgressiveExact(height, preferredExts)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec)
    }

    private fun downloadAtHeight(
        url: String,
        height: Int,
        preferredExts: List<String>,
        noCheckCertificate: Boolean,
        outputTemplate: String?,
        extraArgs: List<String>,
        timeout: Duration?,
        subtitles: SubtitleOptions?,
        onEvent: (Event) -> Unit
    ): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val useCookies = this.cookiesFromBrowser
        val opts = Options(
            format = NetAndArchive.selectorDownloadExact(height, preferredExts),
            outputTemplate = outputTemplate,
            noCheckCertificate = useNoCheckCert,
            cookiesFromBrowser = useCookies,
            extraArgs = extraArgs,
            timeout = timeout,
            subtitles = subtitles
        )
        return download(url, opts, onEvent)
    }

    private fun downloadAtHeightMp4(
        url: String,
        height: Int,
        noCheckCertificate: Boolean,
        outputTemplate: String?,
        extraArgs: List<String>,
        timeout: Duration?,
        recodeIfNeeded: Boolean,
        subtitles: SubtitleOptions?,
        onEvent: (Event) -> Unit
    ): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val useCookies = this.cookiesFromBrowser
        val opts = Options(
            format = NetAndArchive.selectorDownloadExactMp4(height),
            outputTemplate = outputTemplate,
            noCheckCertificate = useNoCheckCert,
            cookiesFromBrowser = useCookies,
            extraArgs = extraArgs,
            timeout = timeout,
            targetContainer = "mp4",
            allowRecode = recodeIfNeeded,
            subtitles = subtitles,
            sponsorBlockRemove = this.sponsorBlockRemove
        )
        return download(url, opts, onEvent)
    }

    private fun downloadAudioInternal(
        url: String,
        outputTemplate: String?,
        noCheckCertificate: Boolean,
        extraArgs: List<String>,
        timeout: Duration?,
        audioArgs: List<String>,
        onEvent: (Event) -> Unit
    ): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val useCookies = this.cookiesFromBrowser
        val options = Options(
            outputTemplate = outputTemplate,
            noCheckCertificate = useNoCheckCert,
            cookiesFromBrowser = useCookies,
            extraArgs = audioArgs + extraArgs,
            timeout = timeout
        )
        return download(url, options, onEvent)
    }

    private suspend fun executeCommand(args: List<String>, timeoutSec: Long): Result<ProcessResult> =
        withContext(Dispatchers.IO) {
            val cmd = buildList {
                add(ytDlpPath)
                addAll(args)
                ffmpegPath?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
            }

            try {
                val process = ProcessBuilder(cmd).start()
                val stdoutReader = async { process.inputStream.bufferedReader(StandardCharsets.UTF_8).readLines() }
                val stderrReader = async { process.errorStream.bufferedReader(StandardCharsets.UTF_8).readText() }

                val exited = withTimeoutOrNull(timeoutSec * 1000) { process.waitFor() }
                if (exited == null) {
                    process.destroyForcibly()
                    return@withContext Result.failure(IllegalStateException("yt-dlp command timed out after ${timeoutSec}s"))
                }

                Result.success(ProcessResult(process.exitValue(), stdoutReader.await(), stderrReader.await()))
            } catch (t: Throwable) {
                Result.failure(IllegalStateException("Failed to start/run yt-dlp process", t))
            }
        }

    /**
     * Lifetime-cached metadata extractor.
     * Keys are the full command line including all flags (so different options cache independently).
     * No TTL; entries persist until process exit or explicit invalidation (binary path change/update)
     * with a bounded LRU (512 entries) to prevent unbounded memory use.
     */
    private suspend fun extractMetadata(
        url: String,
        noCheckCertificate: Boolean,
        timeoutSec: Long,
        extraArgs: List<String>
    ): Result<String> {
        if (!isAvailable()) return Result.failure(IllegalStateException("yt-dlp is not available."))
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val cmdArgs = buildList {
            add("--dump-json"); add("--no-warnings")
            // Aggressively cap network timeouts for faster failures (does not affect cache semantics)
            addAll(listOf("--socket-timeout", "5"))
            if (useNoCheckCert) add("--no-check-certificate")
            cookiesFromBrowser?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--cookies-from-browser", it)) }
            addAll(extraArgs); add(url)
        }

        return withContext(Dispatchers.IO) {
            val fullCmd = buildList { add(ytDlpPath); addAll(cmdArgs) }
            val cacheKey = fullCmd.joinToString("\u0001")

            synchronized(metadataCacheLock) {
                metadataCache[cacheKey]?.let { return@withContext Result.success(it.json) }
            }

            try {
                val pb = ProcessBuilder(fullCmd).redirectErrorStream(false)
                val process = pb.start()
                val jsonDeferred = async { process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText() }
                val errorDeferred = async { process.errorStream.bufferedReader(StandardCharsets.UTF_8).readText() }

                val exited = withTimeoutOrNull(timeoutSec * 1000) { process.waitFor() }
                if (exited == null) {
                    process.destroyForcibly()
                    return@withContext Result.failure(IllegalStateException("Metadata extraction timed out after ${timeoutSec}s"))
                }

                if (process.exitValue() == 0) {
                    val json = jsonDeferred.await()
                    synchronized(metadataCacheLock) {
                        metadataCache[cacheKey] = MetaCacheEntry(json)
                    }
                    Result.success(json)
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Metadata extraction failed (exit ${process.exitValue()})\n${errorDeferred.await()}"
                        )
                    )
                }

            } catch (t: Throwable) {
                Result.failure(IllegalStateException("Failed to start yt-dlp process for metadata", t))
            }
        }
    }

    // --- Subtitle-friendly helpers (unchanged API) ---

    suspend fun getVideoInfoWithAllSubtitles(
        url: String,
        extractFlat: Boolean = false,
        includeAutoSubtitles: Boolean = true,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20,
        maxHeight: Int = 1080,
        preferredExts: List<String> = listOf("mp4", "webm")
    ): Result<VideoInfo> {
        val args = buildList {
            add("--no-playlist")
            if (extractFlat) add("--flat-playlist")
            if (includeAutoSubtitles) add("--write-auto-subs")
        }
        return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json ->
            parseVideoInfoFromJson(json, maxHeight, preferredExts)
        }
    }

    fun downloadSubtitlesOnly(
        url: String,
        languages: List<String>? = null,
        includeAutoSubtitles: Boolean = true,
        subtitleFormat: String = "srt",
        outputDir: File? = null,
        noCheckCertificate: Boolean = false,
        onEvent: (Event) -> Unit
    ): Handle {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val useCookies = this.cookiesFromBrowser

        val subtitleOptions = if (languages == null) {
            SubtitleOptions(
                allSubtitles = true,
                writeAutoSubtitles = includeAutoSubtitles,
                embedSubtitles = false,
                writeSubtitles = true,
                subFormat = subtitleFormat
            )
        } else {
            SubtitleOptions(
                languages = languages,
                writeAutoSubtitles = includeAutoSubtitles,
                embedSubtitles = false,
                writeSubtitles = true,
                subFormat = subtitleFormat
            )
        }

        val extraArgs = listOf("--skip-download")
        val opts = Options(
            noCheckCertificate = useNoCheckCert,
            cookiesFromBrowser = useCookies,
            subtitles = subtitleOptions,
            extraArgs = extraArgs,
            outputTemplate = "%(title)s.%(ext)s"
        )

        val originalDir = downloadDir
        outputDir?.let { downloadDir = it }
        val handle = download(url, opts, onEvent)
        downloadDir = originalDir

        return handle
    }
}
