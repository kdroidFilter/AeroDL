package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.*
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import java.io.File
import java.time.Duration
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
    var downloadDir: File? = File(System.getProperty("user.home"), "Downloads/yt-dlp")

    // Internal engine (composition over inheritance)
    internal val engine by lazy {
        InternalYtDlp(
            ytDlpPathProvider = { ytDlpPath },
            ytDlpPathSetter   = { ytDlpPath = it },
            ffmpegPathProvider= { ffmpegPath },
            ffmpegPathSetter  = { ffmpegPath = it },
            downloadDirProvider = { downloadDir }
        )
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
        fun pct(read: Long, total: Long?): Double? = total?.takeIf { it > 0 }?.let { read * 100.0 / it }
        try {
            onEvent(InitEvent.CheckingYtDlp)
            if (!isAvailable()) {
                onEvent(InitEvent.DownloadingYtDlp)
                if (!downloadOrUpdate { r, t -> onEvent(InitEvent.YtDlpProgress(r, t, pct(r, t))) }) {
                    onEvent(InitEvent.Error("Impossible de télécharger yt-dlp", null))
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
            onEvent(InitEvent.Error(t.message ?: "Erreur d'initialisation", t))
            onEvent(InitEvent.Completed(false))
            return false
        }
    }

    // === Availability / version / update ===
    fun version(): String? = engine.version()
    fun isAvailable(): Boolean = engine.isAvailable()
    suspend fun hasUpdate(): Boolean = engine.hasUpdate()
    suspend fun downloadOrUpdate(onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean = engine.downloadOrUpdate(onProgress)

    // === FFmpeg ===
    fun getDefaultFfmpegPath(): String = PlatformUtils.getDefaultFfmpegPath()
    suspend fun ensureFfmpegAvailable(forceDownload: Boolean = false, onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean = engine.ensureFfmpegAvailable(forceDownload, onProgress)

    // === Network preflight ===
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000): Result<Unit> = engine.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // === Direct URL helpers ===
    fun getDirectUrlForFormat(url: String, formatSelector: String, noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> = engine.getDirectUrlForFormat(url, formatSelector, noCheckCertificate, timeoutSec)
    fun getMediumQualityUrl(url: String, maxHeight: Int = 480, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false): Result<String> = engine.getMediumQualityUrl(url, maxHeight, preferredExts, noCheckCertificate)
    fun getProgressiveUrlMp4(url: String, maxHeight: Int, noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> = engine.getProgressiveUrlMp4AtOrBelow(url, maxHeight, noCheckCertificate, timeoutSec)

    // === Downloader ===
    fun download(url: String, options: Options = Options(), onEvent: (Event) -> Unit): Handle = engine.download(url, options, onEvent)

    // === Simple resolution presets ===
    enum class Preset(val height: Int) { P360(360), P480(480), P720(720), P1080(1080), P1440(1440), P2160(2160) }
    data class ResolutionAvailability(val preset: Preset, val progressive: Boolean, val downloadable: Boolean)

    fun isAvailable(url: String, preset: Preset, noCheckCertificate: Boolean = false): Result<ResolutionAvailability> =
        engine.resolutionAvailability(url, preset.height, noCheckCertificate)
            .map { ResolutionAvailability(preset, it.progressive, it.downloadable) }

    fun probeAvailability(url: String, presets: Array<Preset> = Preset.values(), noCheckCertificate: Boolean = false): Map<Preset, ResolutionAvailability> {
        val lines = engine.listFormatsRaw(url, noCheckCertificate).getOrElse { return emptyMap() }
        val (progressiveHeights, videoOnlyHeights) = io.github.kdroidfilter.ytdlp.util.NetAndArchive.probeAvailableHeights(lines)
        return presets.associateWith { p ->
            ResolutionAvailability(p, p.height in progressiveHeights, (p.height in progressiveHeights) || (p.height in videoOnlyHeights))
        }
    }

    fun getProgressiveUrl(url: String, preset: Preset, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> =
        engine.getProgressiveUrlForHeight(url, preset.height, preferredExts, noCheckCertificate, timeoutSec)

    fun downloadAt(url: String, preset: Preset, outputTemplate: String? = null, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle =
        engine.downloadAtHeight(url, preset.height, preferredExts, noCheckCertificate, outputTemplate, extraArgs, timeout, onEvent)

    fun downloadMp4At(url: String, preset: Preset, outputTemplate: String? = "%(title)s.%(ext)s", noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), recodeIfNeeded: Boolean = false, onEvent: (Event) -> Unit): Handle =
        engine.downloadAtHeightMp4(url, preset.height, noCheckCertificate, outputTemplate, extraArgs, timeout, recodeIfNeeded, onEvent)

    // === Audio downloads (Refactored) ===

    private fun downloadAudioInternal(
        url: String,
        outputTemplate: String?,
        noCheckCertificate: Boolean,
        extraArgs: List<String>,
        timeout: Duration?,
        audioArgs: List<String>,
        onEvent: (Event) -> Unit
    ): Handle {
        val combinedExtraArgs = audioArgs + extraArgs
        val options = Options(
            outputTemplate = outputTemplate,
            noCheckCertificate = noCheckCertificate,
            extraArgs = combinedExtraArgs,
            timeout = timeout
        )
        return engine.download(url, options, onEvent)
    }

    fun downloadAudioMp3(url: String, outputTemplate: String? = "%(title)s.%(ext)s", audioQuality: Int = 0, noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle {
        val args = listOf(
            "--extract-audio", "--audio-format", "mp3",
            "--audio-quality", audioQuality.coerceIn(0, 10).toString(),
            "--add-metadata", "--embed-thumbnail",
            "-f", "bestaudio/best"
        )
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    fun downloadAudioMp3WithBitrate(url: String, bitrate: String = "192k", outputTemplate: String? = "%(title)s.%(ext)s", vbr: Boolean = true, noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle {
        val postprocessorArg = if (vbr) "ffmpeg:-q:a 2" else "ffmpeg:-b:a $bitrate"
        val args = listOf(
            "--extract-audio", "--audio-format", "mp3",
            "--add-metadata", "--embed-thumbnail",
            "-f", "bestaudio/best",
            "--postprocessor-args", postprocessorArg
        )
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    enum class AudioQualityPreset(val bitrate: String, val description: String) {
        LOW("96k", "Économie d'espace - Qualité acceptable pour la parole"),
        MEDIUM("128k", "Qualité standard - Bon pour la musique casual"),
        HIGH("192k", "Haute qualité - Recommandé pour la musique"),
        VERY_HIGH("256k", "Très haute qualité - Excellente fidélité"),
        MAXIMUM("320k", "Qualité maximale - Indiscernable du CD")
    }

    fun downloadAudioMp3WithPreset(url: String, preset: AudioQualityPreset = AudioQualityPreset.HIGH, outputTemplate: String? = "%(title)s.%(ext)s", noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle {
        return downloadAudioMp3WithBitrate(
            url = url, bitrate = preset.bitrate, outputTemplate = outputTemplate, vbr = true,
            noCheckCertificate = noCheckCertificate, extraArgs = extraArgs, timeout = timeout, onEvent = onEvent
        )
    }

    fun getAudioStreamUrl(url: String, preferredCodecs: List<String> = listOf("opus", "m4a", "mp3", "aac", "vorbis"), noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> {
        val codecSelectors = preferredCodecs.joinToString("/") { "bestaudio[acodec=$it]" }
        val formatSelector = "$codecSelectors/bestaudio/best"
        return engine.getDirectUrlForFormat(url, formatSelector, noCheckCertificate, timeoutSec)
    }
}


// === Extension functions for metadata extraction ===
fun YtDlpWrapper.getVideoInfo(url: String, extractFlat: Boolean = false, noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<VideoInfo> {
    return engine.extractVideoInfo(url, extractFlat, noCheckCertificate, timeoutSec)
}

fun YtDlpWrapper.getPlaylistInfo(url: String, extractFlat: Boolean = true, noCheckCertificate: Boolean = false, timeoutSec: Long = 60): Result<PlaylistInfo> {
    return engine.extractPlaylistInfo(url, extractFlat, noCheckCertificate, timeoutSec)
}

fun YtDlpWrapper.getVideoInfoList(url: String, maxEntries: Int? = null, extractFlat: Boolean = true, noCheckCertificate: Boolean = false, timeoutSec: Long = 60): Result<List<VideoInfo>> {
    return engine.extractVideoInfoList(url, maxEntries, extractFlat, noCheckCertificate, timeoutSec)
}