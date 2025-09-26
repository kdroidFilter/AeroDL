package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.Handle
import io.github.kdroidfilter.ytdlp.core.InternalYtDlp
import io.github.kdroidfilter.ytdlp.core.Options
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import java.io.File
import java.time.Duration
import java.time.Duration.ofMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A wrapper class for utilizing the yt-dlp tool functionality.
 * This class provides methods to manage the yt-dlp binary, interact with FFmpeg,
 * perform downloads, and resolve direct media URLs using yt-dlp.
 */
class YtDlpWrapper {

    // === User configuration (externally overridable) ===
    var ytDlpPath: String = PlatformUtils.getDefaultBinaryPath()
    var ffmpegPath: String? = null
    var downloadDir: File? = null

    // Internal engine (composition over inheritance)
    private val engine by lazy {
        InternalYtDlp(
            ytDlpPathProvider = { ytDlpPath },
            ytDlpPathSetter   = { ytDlpPath = it },
            ffmpegPathProvider= { ffmpegPath },
            ffmpegPathSetter  = { ffmpegPath = it },
            downloadDirProvider = { downloadDir }
        )
    }

    init {
        // Set a sensible default download directory if not provided
        if (downloadDir == null) {
            downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
        }
        // No blocking initialization here: call initialize()/initializeIn() from UI if needed.
    }

    // === Initialization events for UI ===
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
     * Non-blocking helper: launch initialization in a provided CoroutineScope.
     * Returns a Job; use onEvent to observe progress in your UI.
     */
    fun initializeIn(scope: CoroutineScope, onEvent: (InitEvent) -> Unit = {}): Job =
        scope.launch { initialize(onEvent) }

    /**
     * Perform on-demand initialization without blocking the calling thread.
     * Suspends while checking/downloading yt-dlp and ensuring FFmpeg.
     * Emits progress through onEvent.
     */
    suspend fun initialize(onEvent: (InitEvent) -> Unit = {}): Boolean {
        fun pct(read: Long, total: Long?): Double? = total?.takeIf { it > 0 }?.let { read.toDouble() * 100.0 / it.toDouble() }
        try {
            onEvent(InitEvent.CheckingYtDlp)
            val available = isAvailable()
            if (!available) {
                onEvent(InitEvent.DownloadingYtDlp)
                if (!downloadOrUpdate { r, t -> onEvent(InitEvent.YtDlpProgress(r, t, pct(r, t))) }) {
                    onEvent(InitEvent.Error("Impossible de télécharger yt-dlp"))
                    onEvent(InitEvent.Completed(false))
                    return false
                }
            } else {
                // Best-effort update check
                try {
                    if (hasUpdate()) {
                        onEvent(InitEvent.UpdatingYtDlp)
                        downloadOrUpdate { r, t -> onEvent(InitEvent.YtDlpProgress(r, t, pct(r, t))) }
                    }
                } catch (t: Throwable) {
                    // ignore update failures, but notify as non-fatal error event
                    onEvent(InitEvent.Error("Échec de la vérification de mise à jour", t))
                }
            }

            onEvent(InitEvent.EnsuringFfmpeg)
            val ffOk = try {
                ensureFfmpegAvailable(false) { r, t -> onEvent(InitEvent.FfmpegProgress(r, t, pct(r, t))) }
            } catch (t: Throwable) { onEvent(InitEvent.Error("FFmpeg indisponible", t)); false }
            val success = ffOk && isAvailable()
            onEvent(InitEvent.Completed(success))
            return success
        } catch (t: Throwable) {
            onEvent(InitEvent.Error(t.message ?: "Erreur d'initialisation", t))
            onEvent(InitEvent.Completed(false))
            return false
        }
    }

    // === Public options (kept identical) ===
    data class Options(
        val format: String? = null,
        val outputTemplate: String? = null,
        val noCheckCertificate: Boolean = false,
        val extraArgs: List<String> = emptyList(),
        val timeout: Duration? = ofMinutes(30)
    )



    // === Availability / version / update ===
    fun version(): String? = engine.version()
    fun isAvailable(): Boolean = engine.isAvailable()

    suspend fun hasUpdate(): Boolean = engine.hasUpdate()

    suspend fun downloadOrUpdate(onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean = engine.downloadOrUpdate(onProgress)

    // === FFmpeg ===
    fun getDefaultFfmpegPath(): String = PlatformUtils.getDefaultFfmpegPath()

    suspend fun ensureFfmpegAvailable(
        forceDownload: Boolean = false,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean = engine.ensureFfmpegAvailable(forceDownload, onProgress)

    suspend fun downloadFfmpeg(
        forceDownload: Boolean = false,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean = engine.downloadFfmpeg(forceDownload, onProgress)

    // === Network preflight (kept) ===
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000)
            : Result<Unit> = engine.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // === Direct URL helpers ===
    fun getDirectUrlForFormat(
        url: String,
        formatSelector: String,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> = engine.getDirectUrlForFormat(url, formatSelector, noCheckCertificate, timeoutSec)

    fun getMediumQualityUrl(
        url: String,
        maxHeight: Int = 480,
        preferredExts: List<String> = listOf("mp4", "webm"),
        noCheckCertificate: Boolean = false
    ): Result<String> = engine.getMediumQualityUrl(url, maxHeight, preferredExts, noCheckCertificate)

    // === Downloader ===
    fun download(
        url: String,
        options: Options = Options(),
        onEvent: (Event) -> Unit
    ): Handle = engine.download(url, options.toCore(), onEvent.toCore())

    // === Simple resolution presets ===
    enum class Preset(val height: Int) {
        P360(360), P480(480), P720(720), P1080(1080), P1440(1440), P2160(2160); // 2K=1440p, 4K=2160p
    }

    data class ResolutionAvailability(
        val preset: Preset,
        val progressive: Boolean,
        val downloadable: Boolean
    )

    /** Check availability for one preset (progressive vs downloadable via merge). */
    fun isAvailable(
        url: String,
        preset: Preset,
        noCheckCertificate: Boolean = false
    ): Result<ResolutionAvailability> {
        val r = engine.resolutionAvailability(url, preset.height, noCheckCertificate)
            .map { ResolutionAvailability(preset, it.progressive, it.downloadable) }
        return r
    }

    /** Bulk check (all presets). */
    fun probeAvailability(
        url: String,
        presets: Array<Preset> = Preset.values(),
        noCheckCertificate: Boolean = false
    ): Map<Preset, ResolutionAvailability> {
        // Run one -F and reuse it for all heights to avoid repeated network calls
        val lines = engine.listFormatsRaw(url, noCheckCertificate).getOrElse { return emptyMap() }
        val (progressiveHeights, videoOnlyHeights) = io.github.kdroidfilter.ytdlp.util.NetAndArchive.probeAvailableHeights(lines)
        return presets.associateWith { p ->
            ResolutionAvailability(
                preset = p,
                progressive = p.height in progressiveHeights,
                downloadable = (p.height in progressiveHeights) || (p.height in videoOnlyHeights)
            )
        }
    }

    /** Get a direct progressive URL at exact preset height (fails if only split A/V exists). */
    fun getProgressiveUrl(
        url: String,
        preset: Preset,
        preferredExts: List<String> = listOf("mp4", "webm"),
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> =
        engine.getProgressiveUrlForHeight(url, preset.height, preferredExts, noCheckCertificate, timeoutSec)

    /** Download at exact preset height. If only split A/V exists, yt-dlp will merge using FFmpeg. */
    fun downloadAt(
        url: String,
        preset: Preset,
        outputTemplate: String? = null,
        preferredExts: List<String> = listOf("mp4", "webm"),
        noCheckCertificate: Boolean = false,
        extraArgs: List<String> = emptyList(),
        timeout: Duration? = ofMinutes(30),
        onEvent: (Event) -> Unit
    ): Handle =
        engine.downloadAtHeight(
            url = url,
            height = preset.height,
            preferredExts = preferredExts,
            noCheckCertificate = noCheckCertificate,
            outputTemplate = outputTemplate,
            extraArgs = extraArgs,
            timeout = timeout,
            onEvent = onEvent
        )

    /** Get a direct progressive MP4 URL at or below the given height (fails if only split A/V exists). */
    fun getProgressiveUrlMp4(
        url: String,
        maxHeight: Int,
        noCheckCertificate: Boolean = false,
        timeoutSec: Long = 20
    ): Result<String> = engine.getProgressiveUrlMp4AtOrBelow(url, maxHeight, noCheckCertificate, timeoutSec)

    /** Download exactly at preset.height into an MP4 file.
     *  If remux is impossible (e.g., incompatible codecs), set recodeIfNeeded=true to force recode. */
    fun downloadMp4At(
        url: String,
        preset: Preset,
        outputTemplate: String? = "%(title)s.%(ext)s",
        noCheckCertificate: Boolean = false,
        extraArgs: List<String> = emptyList(),
        timeout: Duration? = ofMinutes(30),
        recodeIfNeeded: Boolean = false,
        onEvent: (Event) -> Unit
    ): Handle =
        engine.downloadAtHeightMp4(
            url = url,
            height = preset.height,
            noCheckCertificate = noCheckCertificate,
            outputTemplate = outputTemplate,
            extraArgs = extraArgs,
            timeout = timeout,
            recodeIfNeeded = recodeIfNeeded,
            onEvent = onEvent
        )


}

// ---- Mapping to core types (no behavior changes) ----

private fun YtDlpWrapper.Options.toCore(): Options =
    Options(format, outputTemplate, noCheckCertificate, extraArgs, timeout)

private fun ((Event) -> Unit).toCore(): (Event) -> Unit = { e ->
    when (e) {
        is Event.Progress -> this(Event.Progress(e.percent, e.rawLine))
        is Event.Log      -> this(Event.Log(e.line))
        is Event.Error    -> this(Event.Error(e.message, e.cause))
        is Event.Completed-> this(Event.Completed(e.exitCode, e.success))
        Event.Cancelled   -> this(Event.Cancelled)
        Event.Started     -> this(Event.Started)
        is Event.NetworkProblem -> this(Event.NetworkProblem(e.detail))
    }
}
