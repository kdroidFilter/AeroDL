package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.InternalYtDlp
import io.github.kdroidfilter.ytdlp.core.Options
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import java.io.File
import java.time.Duration

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
        val timeout: Duration? = Duration.ofMinutes(30)
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
    ): io.github.kdroidfilter.ytdlp.core.Handle = engine.download(url, options.toCore(), onEvent.toCore())
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
