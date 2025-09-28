package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.Handle
import io.github.kdroidfilter.ytdlp.core.Options
import io.github.kdroidfilter.ytdlp.core.parsePlaylistInfoFromJson
import io.github.kdroidfilter.ytdlp.core.parseVideoInfoFromJson
import io.github.kdroidfilter.ytdlp.util.NetAndArchive
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * Classe principale pour interagir avec l'outil yt-dlp.
 * Gère le binaire, FFmpeg, les téléchargements et l'extraction de métadonnées.
 */
class YtDlpWrapper {

    // === Configuration utilisateur (modifiable de l'extérieur) ===
    var ytDlpPath: String = PlatformUtils.getDefaultBinaryPath()
    var ffmpegPath: String? = null
    var downloadDir: File? = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    /**
     * Si vrai, ajoute l'argument '--no-check-certificate' à toutes les commandes yt-dlp.
     * Peut être outrepassé au cas par cas dans les appels de méthode.
     */
    var noCheckCertificate: Boolean = false

    private val ytdlpFetcher = GitHubReleaseFetcher(owner = "yt-dlp", repo = "yt-dlp")
    private data class ProcessResult(val exitCode: Int, val stdout: List<String>, val stderr: String)

    // === Événements d'initialisation pour l'UI ===
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
     * Lance l'initialisation dans une CoroutineScope fournie.
     * Utilisez onEvent pour observer la progression.
     */
    fun initializeIn(scope: CoroutineScope, onEvent: (InitEvent) -> Unit = {}): Job =
        scope.launch { initialize(onEvent) }

    /**
     * Effectue l'initialisation (vérification/téléchargement de yt-dlp et FFmpeg).
     * S'exécute de manière asynchrone et émet des événements de progression.
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

    // === Disponibilité / version / mise à jour ===
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

    // === FFmpeg ===
    fun getDefaultFfmpegPath(): String = PlatformUtils.getDefaultFfmpegPath()

    suspend fun ensureFfmpegAvailable(forceDownload: Boolean = false, onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
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

    // === Pré-vérification réseau ===
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000): Result<Unit> =
        NetAndArchive.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // === Téléchargeur ===
    fun download(url: String, options: Options = Options(), onEvent: (Event) -> Unit): Handle {
        require(isAvailable()) { "yt-dlp n'est pas disponible." }

        checkNetwork(url, 5000, 5000).getOrElse {
            onEvent(Event.NetworkProblem(it.message ?: "Réseau non disponible"))
            onEvent(Event.Error("La pré-vérification réseau a échoué."))
            return Handle(NetAndArchive.startNoopProcess(), AtomicBoolean(true))
        }

        // Applique le noCheckCertificate global si non spécifié dans les options
        val finalOptions = if (options.noCheckCertificate) options else options.copy(noCheckCertificate = this.noCheckCertificate)
        val cmd = NetAndArchive.buildCommand(ytDlpPath, ffmpegPath, url, finalOptions, downloadDir)
        val process = try {
            ProcessBuilder(cmd).directory(downloadDir).redirectErrorStream(true).start()
        } catch (t: Throwable) {
            onEvent(Event.Error("Échec du démarrage du processus yt-dlp.", t))
            return Handle(NetAndArchive.startNoopProcess(), AtomicBoolean(true))
        }

        val cancelled = AtomicBoolean(false)
        val tail = ArrayBlockingQueue<String>(120)
        onEvent(Event.Started)

        // Thread de lecture
        Thread({
            try {
                process.inputStream.bufferedReader(StandardCharsets.UTF_8).forEachLine { line ->
                    if (!tail.offer(line)) { tail.poll(); tail.offer(line) }
                    val progress = NetAndArchive.parseProgress(line)
                    if (progress != null) onEvent(Event.Progress(progress, line)) else onEvent(Event.Log(line))
                }
            } catch (t: Throwable) {
                if (!cancelled.get()) onEvent(Event.Error("Erreur d'I/O lors de la lecture de la sortie de yt-dlp", t))
            }
        }, "yt-dlp-reader").apply { isDaemon = true }.start()

        // Threads de surveillance (timeout et achèvement)
        startWatchdogThreads(process, finalOptions, cancelled, tail, onEvent)
        return Handle(process, cancelled)
    }

    // === Helpers pour URL directes ===
    fun getDirectUrlForFormat(url: String, formatSelector: String, noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> {
        require(isAvailable()) { "yt-dlp n'est pas disponible." }
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
            return Result.failure(IllegalStateException("yt-dlp -g a échoué (exit ${result.exitCode})\n$errorDetails"))
        }

        val out = result.stdout.map { it.trim() }.filter { it.isNotBlank() }
        return when {
            out.isEmpty() -> Result.failure(IllegalStateException("Aucune URL retournée pour le sélecteur : $formatSelector"))
            out.size == 1 -> Result.success(out.first())
            else -> Result.failure(IllegalStateException("Plusieurs URLs retournées (probablement A/V séparé).\n${out.joinToString("\n")}"))
        }
    }

    // === Presets de résolution simples ===
    enum class Preset(val height: Int) { P360(360), P480(480), P720(720), P1080(1080), P1440(1440), P2160(2160) }
    data class ResolutionAvailability(val preset: Preset, val progressive: Boolean, val downloadable: Boolean)

    fun isAvailable(url: String, preset: Preset, noCheckCertificate: Boolean = false): Result<ResolutionAvailability> =
        resolutionAvailability(url, preset.height, noCheckCertificate)
            .map { ResolutionAvailability(preset, it.progressive, it.downloadable) }

    fun probeAvailability(url: String, presets: Array<Preset> = Preset.values(), noCheckCertificate: Boolean = false): Map<Preset, ResolutionAvailability> {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val lines = listFormatsRaw(url, useNoCheckCert).getOrElse { return emptyMap() }
        val (progressiveHeights, videoOnlyHeights) = NetAndArchive.probeAvailableHeights(lines)
        return presets.associateWith { p ->
            ResolutionAvailability(p, p.height in progressiveHeights, (p.height in progressiveHeights) || (p.height in videoOnlyHeights))
        }
    }

    fun getProgressiveUrl(url: String, preset: Preset, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false, timeoutSec: Long = 20): Result<String> =
        getProgressiveUrlForHeight(url, preset.height, preferredExts, noCheckCertificate, timeoutSec)

    fun downloadAt(url: String, preset: Preset, outputTemplate: String? = null, preferredExts: List<String> = listOf("mp4", "webm"), noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle =
        downloadAtHeight(url, preset.height, preferredExts, noCheckCertificate, outputTemplate, extraArgs, timeout, onEvent)

    fun downloadMp4At(url: String, preset: Preset, outputTemplate: String? = "%(title)s.%(ext)s", noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), recodeIfNeeded: Boolean = false, onEvent: (Event) -> Unit): Handle =
        downloadAtHeightMp4(url, preset.height, noCheckCertificate, outputTemplate, extraArgs, timeout, recodeIfNeeded, onEvent)


    // === Téléchargements audio ===
    fun downloadAudioMp3(url: String, outputTemplate: String? = "%(title)s.%(ext)s", audioQuality: Int = 0, noCheckCertificate: Boolean = false, extraArgs: List<String> = emptyList(), timeout: Duration? = Duration.ofMinutes(30), onEvent: (Event) -> Unit): Handle {
        val args = listOf("--extract-audio", "--audio-format", "mp3", "--audio-quality", audioQuality.coerceIn(0, 10).toString(), "--add-metadata", "--embed-thumbnail", "-f", "bestaudio/best")
        return downloadAudioInternal(url, outputTemplate, noCheckCertificate, extraArgs, timeout, args, onEvent)
    }

    enum class AudioQualityPreset(val bitrate: String, val description: String) {
        LOW("96k", "Économie d'espace"), MEDIUM("128k", "Qualité standard"), HIGH("192k", "Haute qualité"),
        VERY_HIGH("256k", "Très haute qualité"), MAXIMUM("320k", "Qualité maximale")
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

    // === Extraction de métadonnées ===
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
    // =================== MÉTHODES PRIVÉES INTERNES ===================
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

    private data class ResolutionAvailabilityInternal(val height: Int, val progressive: Boolean, val downloadable: Boolean)

    private fun resolutionAvailability(url: String, height: Int, noCheckCertificate: Boolean): Result<ResolutionAvailabilityInternal> {
        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        return listFormatsRaw(url, useNoCheckCert).map { list ->
            val (progressiveHeights, videoOnlyHeights) = NetAndArchive.probeAvailableHeights(list)
            ResolutionAvailabilityInternal(height = height, progressive = height in progressiveHeights, downloadable = (height in progressiveHeights) || (height in videoOnlyHeights))
        }
    }

    private fun listFormatsRaw(url: String, noCheckCertificate: Boolean): Result<List<String>> {
        require(isAvailable()) { "yt-dlp n'est pas disponible." }
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val args = buildList {
            addAll(listOf("-F", "--no-playlist"))
            if (useNoCheckCert) add("--no-check-certificate")
            add(url)
        }
        val result = executeCommand(args, 60).getOrElse { return Result.failure(it) }

        return if (result.exitCode == 0) Result.success(result.stdout)
        else Result.failure(IllegalStateException("yt-dlp -F a échoué (exit ${result.exitCode})\n${result.stderr}"))
    }

    private fun startWatchdogThreads(process: Process, options: Options, cancelled: AtomicBoolean, tail: ArrayBlockingQueue<String>, onEvent: (Event) -> Unit) {
        options.timeout?.let { limit ->
            Thread({
                try {
                    if (!process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS) && !cancelled.getAndSet(true)) {
                        process.destroy()
                        onEvent(Event.Error("Le téléchargement a expiré après ${limit.toMinutes()} minutes."))
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
                val tailPreview = if (lines.isEmpty()) "(aucune sortie capturée)" else lines.takeLast(min(15, lines.size)).joinToString("\n")
                onEvent(Event.Error("yt-dlp a échoué (exit $exit). ${diagnostic ?: ""}".trim() + "\n--- Dernière sortie ---\n$tailPreview"))
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
        catch (t: Throwable) { return Result.failure(IllegalStateException("Échec du démarrage du processus yt-dlp", t)) }

        val stdoutLines = mutableListOf<String>()
        val stderr = StringBuilder()
        val outReader = Thread { process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.forEachLine(stdoutLines::add) } }
        val errReader = Thread { process.errorStream.bufferedReader(StandardCharsets.UTF_8).use { reader -> reader.forEachLine { stderr.appendLine(it) } } }
        outReader.start(); errReader.start()

        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            outReader.interrupt(); errReader.interrupt()
            return Result.failure(IllegalStateException("La commande yt-dlp a expiré après ${timeoutSec}s"))
        }

        outReader.join(2000); errReader.join(2000)
        return Result.success(ProcessResult(process.exitValue(), stdoutLines, stderr.toString()))
    }

    private fun extractMetadata(url: String, noCheckCertificate: Boolean, timeoutSec: Long, extraArgs: List<String>): Result<String> {
        require(isAvailable()) { "yt-dlp n'est pas disponible." }
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val useNoCheckCert = noCheckCertificate || this.noCheckCertificate
        val cmdArgs = buildList {
            add("--dump-json"); add("--no-warnings")
            if (useNoCheckCert) add("--no-check-certificate")
            addAll(extraArgs); add(url)
        }
        val pb = ProcessBuilder(buildList { add(ytDlpPath); addAll(cmdArgs) }).redirectErrorStream(false)
        val process = try { pb.start() }
        catch (t: Throwable) { return Result.failure(IllegalStateException("Échec du démarrage du processus yt-dlp", t)) }

        val jsonOutput = StringBuilder()
        val errorOutput = StringBuilder()
        val outputReader = Thread { process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { it.forEach(jsonOutput::appendLine) } }
        val errorReader = Thread { process.errorStream.bufferedReader(StandardCharsets.UTF_8).useLines { it.forEach(errorOutput::appendLine) } }
        outputReader.start(); errorReader.start()

        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return Result.failure(IllegalStateException("L'extraction des métadonnées a expiré après ${timeoutSec}s"))
        }
        outputReader.join(5000); errorReader.join(5000)

        return if (process.exitValue() == 0) Result.success(jsonOutput.toString())
        else Result.failure(IllegalStateException("L'extraction des métadonnées a échoué (exit ${process.exitValue()})\n${errorOutput}"))
    }
}