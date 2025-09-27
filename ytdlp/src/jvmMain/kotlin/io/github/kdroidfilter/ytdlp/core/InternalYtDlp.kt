package io.github.kdroidfilter.ytdlp.core

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.model.*
import io.github.kdroidfilter.ytdlp.util.NetAndArchive
import io.github.kdroidfilter.ytdlp.util.PlatformUtils
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class InternalYtDlp(
    val ytDlpPathProvider: () -> String,
    private val ytDlpPathSetter: (String) -> Unit,
    internal val ffmpegPathProvider: () -> String?,
    private val ffmpegPathSetter: (String?) -> Unit,
    private val downloadDirProvider: () -> java.io.File?
) {
    private val ytdlpFetcher = GitHubReleaseFetcher(owner = "yt-dlp", repo = "yt-dlp")
    private data class ProcessResult(val exitCode: Int, val stdout: List<String>, val stderr: String)

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

    suspend fun downloadOrUpdate(onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        val assetName = PlatformUtils.getYtDlpAssetNameForSystem()
        val destFile = java.io.File(PlatformUtils.getDefaultBinaryPath())
        destFile.parentFile?.mkdirs()

        return try {
            val release = ytdlpFetcher.getLatestRelease() ?: return false
            val asset = release.assets.find { it.name == assetName } ?: return false

            println("Downloading yt-dlp: ${asset.name} (${release.tag_name})")
            PlatformUtils.downloadFile(asset.browser_download_url, destFile, onProgress)

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
    suspend fun ensureFfmpegAvailable(forceDownload: Boolean, onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        // Vérifie si un chemin ffmpeg est déjà fourni et valide
        ffmpegPathProvider()?.let {
            if (PlatformUtils.ffmpegVersion(it) != null && !forceDownload) return true
        }
        // Cherche ffmpeg dans le PATH du système
        PlatformUtils.findFfmpegInSystemPath()?.let {
            ffmpegPathSetter(it)
            return true
        }
        // Sinon, tente de le télécharger pour les systèmes d'exploitation pris en charge
        return when (getOperatingSystem()) {
            OperatingSystem.WINDOWS, OperatingSystem.LINUX -> downloadFfmpeg(forceDownload, onProgress)
            // Pour MACOS et tout autre OS, le téléchargement automatique n'est pas pris en charge
            else -> false
        }
    }

    private suspend fun downloadFfmpeg(forceDownload: Boolean, onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        val asset = PlatformUtils.getFfmpegAssetNameForSystem() ?: return false
        val result = PlatformUtils.downloadAndInstallFfmpeg(asset, forceDownload, onProgress)
        if (result != null) ffmpegPathSetter(result)
        return result != null
    }

    // -------- Network ----------
    fun checkNetwork(targetUrl: String, connectTimeoutMs: Int, readTimeoutMs: Int): Result<Unit> =
        NetAndArchive.checkNetwork(targetUrl, connectTimeoutMs, readTimeoutMs)

    // -------- Command Execution Engine (Refactored) ----------
    private fun executeCommand(
        args: List<String>,
        timeoutSec: Long
    ): Result<ProcessResult> {
        val cmd = buildList {
            add(ytDlpPathProvider())
            addAll(args)
            ffmpegPathProvider()?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
        }

        val process: Process
        try {
            process = ProcessBuilder(cmd).start()
        } catch (t: Throwable) {
            return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
        }

        val stdoutLines = mutableListOf<String>()
        val stderr = StringBuilder()
        val outReader = Thread {
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.forEachLine(stdoutLines::add) }
        }
        val errReader = Thread {
            process.errorStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                reader.forEachLine { stderr.appendLine(it) }
            }
        }
        outReader.start()
        errReader.start()

        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            outReader.interrupt()
            errReader.interrupt()
            return Result.failure(IllegalStateException("yt-dlp command timed out after ${timeoutSec}s"))
        }

        outReader.join(2000)
        errReader.join(2000)

        return Result.success(ProcessResult(process.exitValue(), stdoutLines, stderr.toString()))
    }

    // -------- Direct URL (no download) ----------
    fun getDirectUrlForFormat(
        url: String,
        formatSelector: String,
        noCheckCertificate: Boolean,
        timeoutSec: Long
    ): Result<String> {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val args = buildList {
            addAll(listOf("-f", formatSelector, "-g", "--no-playlist", "--newline"))
            if (noCheckCertificate) add("--no-check-certificate")
            add(url)
        }

        val result = executeCommand(args, timeoutSec).getOrElse { return Result.failure(it) }

        if (result.exitCode != 0) {
            val errPreview = result.stdout.takeLast(10).joinToString("\n")
            return Result.failure(IllegalStateException("yt-dlp -g failed (exit ${result.exitCode})\n$errPreview"))
        }

        val out = result.stdout.map { it.trim() }.filter { it.isNotBlank() }
        return when (out.size) {
            0 -> Result.failure(IllegalStateException("No URL returned by yt-dlp for selector: $formatSelector"))
            1 -> Result.success(out.first())
            else -> Result.failure(IllegalStateException("Multiple URLs returned (likely split A/V). Refusing to avoid merging.\n${out.joinToString("\n")}"))
        }
    }

    fun getMediumQualityUrl(url: String, maxHeight: Int, preferredExts: List<String>, noCheckCertificate: Boolean): Result<String> {
        val selector = NetAndArchive.progressiveMediumSelector(maxHeight, preferredExts)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec = 20)
    }

    fun getProgressiveUrlMp4AtOrBelow(url: String, maxHeight: Int, noCheckCertificate: Boolean, timeoutSec: Long): Result<String> {
        val selector = NetAndArchive.progressiveMediumSelectorMp4(maxHeight)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec)
    }

    // -------- Download with streaming events ----------
    fun download(url: String, options: Options, onEvent: (Event) -> Unit): Handle {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

        checkNetwork(url, 5000, 5000).getOrElse {
            onEvent(Event.NetworkProblem(it.message ?: "Network not available"))
            onEvent(Event.Error("Network preflight failed."))
            return Handle(NetAndArchive.startNoopProcess(), AtomicBoolean(true))
        }

        val cmd = NetAndArchive.buildCommand(ytDlpPathProvider(), ffmpegPathProvider(), url, options, downloadDirProvider())
        val pb = ProcessBuilder(cmd).directory(downloadDirProvider()).redirectErrorStream(true)

        val process = try {
            pb.start()
        } catch (t: Throwable) {
            onEvent(Event.Error("Failed to start yt-dlp process (permissions or path issue).", t))
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

        // Timeout and Completion watchers
        startWatchdogThreads(process, options, cancelled, tail, onEvent)

        return Handle(process, cancelled)
    }

    private fun startWatchdogThreads(process: Process, options: Options, cancelled: AtomicBoolean, tail: ArrayBlockingQueue<String>, onEvent: (Event) -> Unit) {
        options.timeout?.let { limit ->
            Thread({
                try {
                    if (!process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS)) {
                        if (!cancelled.getAndSet(true)) {
                            process.destroy()
                            onEvent(Event.Error("Download timed out after ${limit.toMinutes()} minutes."))
                        }
                    }
                } catch (_: InterruptedException) { /* Allow thread to exit */ }
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

    // ===== Formats / availability =====
    fun listFormatsRaw(url: String, noCheckCertificate: Boolean): Result<List<String>> {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }
        checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

        val args = buildList {
            addAll(listOf("-F", "--no-playlist"))
            if (noCheckCertificate) add("--no-check-certificate")
            add(url)
        }

        val result = executeCommand(args, 60).getOrElse { return Result.failure(it) }
        return if (result.exitCode == 0) Result.success(result.stdout)
        else Result.failure(IllegalStateException("yt-dlp -F failed (exit ${result.exitCode})"))
    }

    data class ResolutionAvailability(val height: Int, val progressive: Boolean, val downloadable: Boolean)

    fun resolutionAvailability(url: String, height: Int, noCheckCertificate: Boolean): Result<ResolutionAvailability> {
        return listFormatsRaw(url, noCheckCertificate).map { list ->
            val (progressiveHeights, videoOnlyHeights) = NetAndArchive.probeAvailableHeights(list)
            ResolutionAvailability(
                height = height,
                progressive = height in progressiveHeights,
                downloadable = (height in progressiveHeights) || (height in videoOnlyHeights)
            )
        }
    }

    fun getProgressiveUrlForHeight(url: String, height: Int, preferredExts: List<String>, noCheckCertificate: Boolean, timeoutSec: Long): Result<String> {
        val selector = NetAndArchive.selectorProgressiveExact(height, preferredExts)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec)
    }

    fun downloadAtHeight(url: String, height: Int, preferredExts: List<String>, noCheckCertificate: Boolean, outputTemplate: String?, extraArgs: List<String>, timeout: java.time.Duration?, onEvent: (Event) -> Unit): Handle {
        val opts = Options(
            format = NetAndArchive.selectorDownloadExact(height, preferredExts),
            outputTemplate = outputTemplate, noCheckCertificate = noCheckCertificate,
            extraArgs = extraArgs, timeout = timeout
        )
        return download(url, opts, onEvent)
    }

    fun downloadAtHeightMp4(url: String, height: Int, noCheckCertificate: Boolean, outputTemplate: String?, extraArgs: List<String>, timeout: java.time.Duration?, recodeIfNeeded: Boolean, onEvent: (Event) -> Unit): Handle {
        val opts = Options(
            format = NetAndArchive.selectorDownloadExactMp4(height),
            outputTemplate = outputTemplate, noCheckCertificate = noCheckCertificate,
            extraArgs = extraArgs, timeout = timeout,
            targetContainer = "mp4", allowRecode = recodeIfNeeded
        )
        return download(url, opts, onEvent)
    }
}

// ===== Metadata Extraction (Refactored) =====
private fun InternalYtDlp.extractMetadata(
    url: String,
    noCheckCertificate: Boolean,
    timeoutSec: Long,
    extraArgs: List<String>
): Result<String> {
    require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }
    checkNetwork(url, 5000, 5000).getOrElse { return Result.failure(it) }

    val cmdArgs = buildList {
        add("--dump-json")
        add("--no-warnings")
        if (noCheckCertificate) add("--no-check-certificate")
        addAll(extraArgs)
        add(url)
    }

    val pb = ProcessBuilder(buildList { add(ytDlpPathProvider()); addAll(cmdArgs) })
        .redirectErrorStream(false)

    val process = try { pb.start() } catch (t: Throwable) {
        return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
    }

    val jsonOutput = StringBuilder()
    val errorOutput = StringBuilder()

    // LA CORRECTION EST ICI : appendLine au lieu de append
    val outputReader = Thread { process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { it.forEach(jsonOutput::appendLine) } }

    val errorReader = Thread { process.errorStream.bufferedReader(StandardCharsets.UTF_8).useLines { it.forEach(errorOutput::appendLine) } }

    outputReader.start(); errorReader.start()

    if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return Result.failure(IllegalStateException("yt-dlp metadata extraction timed out after ${timeoutSec}s"))
    }
    outputReader.join(5000); errorReader.join(5000)

    return if (process.exitValue() == 0) Result.success(jsonOutput.toString())
    else Result.failure(IllegalStateException("yt-dlp metadata extraction failed (exit ${process.exitValue()})\n${errorOutput}"))
}

fun InternalYtDlp.extractVideoInfo(url: String, extractFlat: Boolean, noCheckCertificate: Boolean, timeoutSec: Long): Result<VideoInfo> {
    val args = buildList {
        add("--no-playlist")
        if (extractFlat) add("--flat-playlist")
    }
    return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json -> parseVideoInfoFromJson(json) }
}

fun InternalYtDlp.extractPlaylistInfo(url: String, extractFlat: Boolean, noCheckCertificate: Boolean, timeoutSec: Long): Result<PlaylistInfo> {
    val args = if (extractFlat) listOf("--flat-playlist") else emptyList()
    return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { json -> parsePlaylistInfoFromJson(json) }
}

fun InternalYtDlp.extractVideoInfoList(url: String, maxEntries: Int?, extractFlat: Boolean, noCheckCertificate: Boolean, timeoutSec: Long): Result<List<VideoInfo>> {
    val args = buildList {
        if (extractFlat) add("--flat-playlist")
        maxEntries?.let { addAll(listOf("--playlist-end", it.toString())) }
    }
    return extractMetadata(url, noCheckCertificate, timeoutSec, args).mapCatching { jsonString ->
        val jsonLines = jsonString.lines().filter { it.isNotBlank() }
        if (jsonLines.size == 1 && jsonLines[0].trim().startsWith("{\"_type\":\"playlist\"")) {
            parsePlaylistInfoFromJson(jsonLines[0]).entries
        } else {
            jsonLines.map { line -> parseVideoInfoFromJson(line) }
        }
    }
}

// --- JSON Parsers (no changes needed, already robust) ---
private fun parseVideoInfoFromJson(jsonString: String): VideoInfo {
    // Helpers kept local to avoid leaking symbols:
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull
    fun JsonElement?.longOrNull() = this?.jsonPrimitive?.longOrNull
    fun JsonElement?.doubleOrNull() = this?.jsonPrimitive?.doubleOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try {
        json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (_: Exception) { buildJsonObject { } }

    val id = root["id"].strOrNull() ?: root["url"].strOrNull() ?: ""
    val url = root["url"].strOrNull() ?: root["webpage_url"].strOrNull() ?: ""
    val title = root["title"].strOrNull() ?: "Unknown"
    val duration = root["duration"].doubleOrNull()?.let { java.time.Duration.ofSeconds(it.toLong()) }
    val uploader = root["uploader"].strOrNull() ?: root["channel"].strOrNull()
    val uploaderUrl = root["uploader_url"].strOrNull() ?: root["channel_url"].strOrNull()

    val availableSubtitles: Map<String, SubtitleInfo> = buildMap {
        root["subtitles"].objOrNull()?.forEach { (lang, dataEl) ->
            val arr = dataEl.arrOrNull() ?: return@forEach
            val formats = arr.mapNotNull { fe ->
                val fo = fe.objOrNull() ?: return@mapNotNull null
                val ext = fo["ext"].strOrNull() ?: return@mapNotNull null
                SubtitleFormat(ext = ext, url = fo["url"].strOrNull(), name = fo["name"].strOrNull())
            }
            if (formats.isNotEmpty()) {
                put(lang, SubtitleInfo(language = lang, languageName = arr.firstOrNull()?.objOrNull()?.get("name")?.strOrNull(), formats = formats))
            }
        }
    }

    val chapters: List<ChapterInfo> = root["chapters"].arrOrNull()?.mapNotNull { el ->
        val o = el.objOrNull() ?: return@mapNotNull null
        val start = o["start_time"].doubleOrNull() ?: return@mapNotNull null
        val end = o["end_time"].doubleOrNull() ?: return@mapNotNull null
        ChapterInfo(title = o["title"].strOrNull(), startTime = start, endTime = end)
    } ?: emptyList()

    return VideoInfo(
        id = id, title = title, url = url, thumbnail = root["thumbnail"].strOrNull(),
        duration = duration, description = root["description"].strOrNull(), uploader = uploader,
        uploaderUrl = uploaderUrl, uploadDate = root["upload_date"].strOrNull(),
        viewCount = root["view_count"].longOrNull(), likeCount = root["like_count"].longOrNull(),
        width = root["width"].intOrNull(), height = root["height"].intOrNull(), fps = root["fps"].doubleOrNull(),
        formatNote = root["format_note"].strOrNull(),
        availableSubtitles = availableSubtitles, chapters = chapters,
        tags = (root["tags"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList(),
        categories = (root["categories"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList()
    )
}

private fun parsePlaylistInfoFromJson(jsonString: String): PlaylistInfo {
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val root = try { json.parseToJsonElement(jsonString).objOrNull() ?: buildJsonObject { }
    } catch (_: Exception) { buildJsonObject { } }

    val entries: List<VideoInfo> = root["entries"].arrOrNull()
        ?.mapNotNull { el ->
            try { parseVideoInfoFromJson(el.objOrNull().toString()) } catch (_: Exception) { null }
        } ?: emptyList()

    return PlaylistInfo(
        id = root["id"].strOrNull(), title = root["title"].strOrNull(), description = root["description"].strOrNull(),
        uploader = root["uploader"].strOrNull(), uploaderUrl = root["uploader_url"].strOrNull(),
        entries = entries, entryCount = root["playlist_count"].intOrNull() ?: entries.size
    )
}