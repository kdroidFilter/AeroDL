package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.Handle
import io.github.kdroidfilter.ytdlp.core.InternalYtDlp
import io.github.kdroidfilter.ytdlp.core.Options
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import java.io.File
import java.time.Duration
import java.time.Duration.ofMinutes

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

    suspend fun downloadOrUpdate(): Boolean = engine.downloadOrUpdate()

    // === FFmpeg ===
    fun getDefaultFfmpegPath(): String = PlatformUtils.getDefaultFfmpegPath()

    suspend fun ensureFfmpegAvailable(forceDownload: Boolean = false): Boolean =
        engine.ensureFfmpegAvailable(forceDownload)

    suspend fun downloadFfmpeg(forceDownload: Boolean = false): Boolean =
        engine.downloadFfmpeg(forceDownload)

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
