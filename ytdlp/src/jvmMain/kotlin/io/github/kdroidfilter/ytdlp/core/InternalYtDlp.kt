package io.github.kdroidfilter.ytdlp.core

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.model.ChapterInfo
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.SubtitleFormat
import io.github.kdroidfilter.ytdlp.model.SubtitleInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlp.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Duration
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
        ffmpegPathProvider()?.let { if (PlatformUtils.ffmpegVersion(it) != null && !forceDownload) return true }
        PlatformUtils.findFfmpegInSystemPath()?.let { ffmpegPathSetter(it); return true }
        return when (getOperatingSystem()) {
            OperatingSystem.WINDOWS, OperatingSystem.LINUX -> downloadFfmpeg(forceDownload, onProgress)
            OperatingSystem.MACOS -> false
            else -> false
        }
    }

    suspend fun downloadFfmpeg(forceDownload: Boolean, onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null): Boolean {
        val asset = PlatformUtils.getFfmpegAssetNameForSystem() ?: return false
        val result = PlatformUtils.downloadAndInstallFfmpeg(asset, forceDownload, onProgress)
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

    fun getProgressiveUrlMp4AtOrBelow(
        url: String,
        maxHeight: Int,
        noCheckCertificate: Boolean,
        timeoutSec: Long = 20
    ): Result<String> {
        val selector = NetAndArchive.progressiveMediumSelectorMp4(maxHeight)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec)
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

    // ===== Formats / availability =====

    /** Run `yt-dlp -F` and return raw lines (stdout merged). */
    fun listFormatsRaw(
        url: String,
        noCheckCertificate: Boolean
    ): Result<List<String>> {
        require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

        val net = checkNetwork(url, 5000, 5000)
        if (net.isFailure) return Result.failure(IllegalStateException("Network preflight failed: ${net.exceptionOrNull()?.message}"))

        val cmd = buildList {
            add(ytDlpPathProvider())
            addAll(listOf("-F", "--no-playlist"))
            if (noCheckCertificate) add("--no-check-certificate")
            add(url)
        }

        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        val process = try { pb.start() } catch (t: Throwable) {
            return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
        }

        val outLines = process.inputStream.bufferedReader(Charsets.UTF_8).readLines()
        val exit = process.waitFor()
        if (exit != 0) return Result.failure(IllegalStateException("yt-dlp -F failed (exit $exit)"))
        return Result.success(outLines)
    }

    data class ResolutionAvailability(
        val height: Int,
        /** single progressive A+V stream exists at this exact height */
        val progressive: Boolean,
        /** at least a video-only stream exists (yt-dlp can merge with audio on download) */
        val downloadable: Boolean
    )

    /** Check availability using `-F` (progressive vs video-only) at exact height. */
    fun resolutionAvailability(
        url: String,
        height: Int,
        noCheckCertificate: Boolean
    ): Result<ResolutionAvailability> {
        val list = listFormatsRaw(url, noCheckCertificate).getOrElse { return Result.failure(it) }
        val (progressiveHeights, videoOnlyHeights) = NetAndArchive.probeAvailableHeights(list)
        return Result.success(
            ResolutionAvailability(
                height = height,
                progressive = height in progressiveHeights,
                downloadable = (height in progressiveHeights) || (height in videoOnlyHeights)
            )
        )
    }

    /** Direct progressive URL (exact height). Returns failure if only split A/V exists. */
    fun getProgressiveUrlForHeight(
        url: String,
        height: Int,
        preferredExts: List<String>,
        noCheckCertificate: Boolean,
        timeoutSec: Long = 20
    ): Result<String> {
        val selector = NetAndArchive.selectorProgressiveExact(height, preferredExts)
        return getDirectUrlForFormat(url, selector, noCheckCertificate, timeoutSec)
    }

    /** Download at exact height (split A/V merge if needed; falls back to progressive exact). */
    fun downloadAtHeight(
        url: String,
        height: Int,
        preferredExts: List<String>,
        noCheckCertificate: Boolean,
        outputTemplate: String?,
        extraArgs: List<String>,
        timeout: java.time.Duration?,
        onEvent: (Event) -> Unit
    ): Handle {
        val format = NetAndArchive.selectorDownloadExact(height, preferredExts)
        val opts = Options(
            format = format,
            outputTemplate = outputTemplate,
            noCheckCertificate = noCheckCertificate,
            extraArgs = extraArgs,
            timeout = timeout
        )
        return download(url, opts, onEvent)
    }

    fun downloadAtHeightMp4(
        url: String,
        height: Int,
        noCheckCertificate: Boolean,
        outputTemplate: String?,
        extraArgs: List<String>,
        timeout: java.time.Duration?,
        recodeIfNeeded: Boolean,
        onEvent: (Event) -> Unit
    ): Handle {
        val format = NetAndArchive.selectorDownloadExactMp4(height)
        val opts = Options(
            format = format,
            outputTemplate = outputTemplate,
            noCheckCertificate = noCheckCertificate,
            extraArgs = extraArgs,
            timeout = timeout,
            targetContainer = "mp4",
            allowRecode = recodeIfNeeded
        )
        return download(url, opts, onEvent)
    }

}

fun InternalYtDlp.extractVideoInfo(
    url: String,
    extractFlat: Boolean,
    noCheckCertificate: Boolean,
    timeoutSec: Long
): Result<VideoInfo> {
    require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

    val net = checkNetwork(url, 5000, 5000)
    if (net.isFailure) return Result.failure(IllegalStateException("Network preflight failed: ${net.exceptionOrNull()?.message}"))

    val cmd = buildList {
        add(ytDlpPathProvider())
        add("--dump-json")
        add("--no-warnings")
        add("--no-playlist")
        if (extractFlat) add("--flat-playlist")
        if (noCheckCertificate) add("--no-check-certificate")
        ffmpegPathProvider()?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
        add(url)
    }

    val pb = ProcessBuilder(cmd).redirectErrorStream(false)
    val process = try { pb.start() } catch (t: Throwable) {
        return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
    }

    // Lire les streams dans des threads séparés pour éviter le deadlock
    val jsonOutput = StringBuilder()
    val errorOutput = StringBuilder()

    val outputReader = Thread {
        process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.forEachLine { jsonOutput.append(it) }
        }
    }

    val errorReader = Thread {
        process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.forEachLine { errorOutput.append(it).append("\n") }
        }
    }

    outputReader.start()
    errorReader.start()

    val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)

    if (!finished) {
        process.destroy()
        outputReader.interrupt()
        errorReader.interrupt()
        return Result.failure(IllegalStateException("yt-dlp metadata extraction timed out after ${timeoutSec}s"))
    }

    // Attendre que les threads de lecture se terminent
    outputReader.join(5000)
    errorReader.join(5000)

    val exit = process.exitValue()

    if (exit != 0) {
        return Result.failure(IllegalStateException("yt-dlp metadata extraction failed (exit $exit)\n${errorOutput}"))
    }

    return try {
        val videoInfo = parseVideoInfoFromJson(jsonOutput.toString())
        Result.success(videoInfo)
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Failed to parse video metadata JSON", e))
    }
}

fun InternalYtDlp.extractPlaylistInfo(
    url: String,
    extractFlat: Boolean,
    noCheckCertificate: Boolean,
    timeoutSec: Long
): Result<PlaylistInfo> {
    require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

    val net = checkNetwork(url, 5000, 5000)
    if (net.isFailure) return Result.failure(IllegalStateException("Network preflight failed: ${net.exceptionOrNull()?.message}"))

    val cmd = buildList {
        add(ytDlpPathProvider())
        add("--dump-json")
        if (extractFlat) add("--flat-playlist")
        if (noCheckCertificate) add("--no-check-certificate")
        ffmpegPathProvider()?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
        add(url)
    }

    val pb = ProcessBuilder(cmd).redirectErrorStream(false)
    val process = try { pb.start() } catch (t: Throwable) {
        return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
    }

    // Lire les streams dans des threads séparés
    val jsonOutput = StringBuilder()
    val errorOutput = StringBuilder()

    val outputReader = Thread {
        try {
            process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().forEach { line ->
                    jsonOutput.append(line)
                }
            }
        } catch (e: Exception) {
            // Thread interrompu ou erreur I/O
        }
    }.apply { start() }

    val errorReader = Thread {
        try {
            process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().forEach { line ->
                    errorOutput.appendLine(line)
                }
            }
        } catch (e: Exception) {
            // Thread interrompu ou erreur I/O
        }
    }.apply { start() }

    val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)

    if (!finished) {
        process.destroyForcibly()
        outputReader.interrupt()
        errorReader.interrupt()
        return Result.failure(IllegalStateException("yt-dlp playlist extraction timed out after ${timeoutSec}s"))
    }

    outputReader.join(5000)
    errorReader.join(5000)

    val exit = process.exitValue()

    if (exit != 0) {
        return Result.failure(IllegalStateException("yt-dlp playlist extraction failed (exit $exit)\n$errorOutput"))
    }

    return try {
        val playlistInfo = parsePlaylistInfoFromJson(jsonOutput.toString())
        Result.success(playlistInfo)
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Failed to parse playlist metadata JSON", e))
    }
}

fun InternalYtDlp.extractVideoInfoList(
    url: String,
    maxEntries: Int?,
    extractFlat: Boolean,
    noCheckCertificate: Boolean,
    timeoutSec: Long
): Result<List<VideoInfo>> {
    require(isAvailable()) { "yt-dlp is not available. Call downloadOrUpdate() first or set ytDlpPath." }

    val net = checkNetwork(url, 5000, 5000)
    if (net.isFailure) return Result.failure(IllegalStateException("Network preflight failed: ${net.exceptionOrNull()?.message}"))

    val cmd = buildList {
        add(ytDlpPathProvider())
        add("--dump-json")
        if (extractFlat) add("--flat-playlist")
        maxEntries?.let { addAll(listOf("--playlist-end", it.toString())) }
        if (noCheckCertificate) add("--no-check-certificate")
        ffmpegPathProvider()?.takeIf { it.isNotBlank() }?.let { addAll(listOf("--ffmpeg-location", it)) }
        add(url)
    }

    val pb = ProcessBuilder(cmd).redirectErrorStream(false)
    val process = try { pb.start() } catch (t: Throwable) {
        return Result.failure(IllegalStateException("Failed to start yt-dlp process", t))
    }

    // Lire les streams dans des threads séparés
    val jsonLines = mutableListOf<String>()
    val errorOutput = StringBuilder()

    val outputReader = Thread {
        try {
            process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().forEach { line ->
                    synchronized(jsonLines) {
                        jsonLines.add(line)
                    }
                }
            }
        } catch (e: Exception) {
            // Thread interrompu ou erreur I/O
        }
    }.apply { start() }

    val errorReader = Thread {
        try {
            process.errorStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.lineSequence().forEach { line ->
                    errorOutput.appendLine(line)
                }
            }
        } catch (e: Exception) {
            // Thread interrompu ou erreur I/O
        }
    }.apply { start() }

    val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)

    if (!finished) {
        process.destroyForcibly()
        outputReader.interrupt()
        errorReader.interrupt()
        return Result.failure(IllegalStateException("yt-dlp extraction timed out after ${timeoutSec}s"))
    }

    outputReader.join(5000)
    errorReader.join(5000)

    val exit = process.exitValue()

    if (exit != 0) {
        return Result.failure(IllegalStateException("yt-dlp extraction failed (exit $exit)\n$errorOutput"))
    }

    return try {
        // Check if it's a single JSON object (playlist) or multiple JSON objects (entries)
        val videos = if (jsonLines.size == 1 && jsonLines[0].trim().startsWith("{\"_type\":\"playlist\"")) {
            // It's a playlist JSON
            val playlistInfo = parsePlaylistInfoFromJson(jsonLines[0])
            playlistInfo.entries
        } else {
            // Multiple JSON objects, one per line
            jsonLines.filter { it.isNotBlank() }.map { line ->
                parseVideoInfoFromJson(line)
            }
        }
        Result.success(videos)
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Failed to parse video list JSON", e))
    }
}

// --- Fully safe: parse a single video JSON blob ---
private fun parseVideoInfoFromJson(jsonString: String): VideoInfo {
    // Helpers kept local to avoid leaking symbols:
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull
    fun JsonElement?.longOrNull() = this?.jsonPrimitive?.longOrNull
    fun JsonElement?.doubleOrNull() = this?.jsonPrimitive?.doubleOrNull

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val root = try {
        json.parseToJsonElement(jsonString).objOrNull() ?: kotlinx.serialization.json.buildJsonObject { }
    } catch (_: Exception) {
        kotlinx.serialization.json.buildJsonObject { }
    }

    // Fallbacks for URL/id/title: yt-dlp sometimes places canonical URL in webpage_url only
    val id = root["id"].strOrNull()
        ?: root["url"].strOrNull()
        ?: ""
    val url = root["url"].strOrNull()
        ?: root["webpage_url"].strOrNull()
        ?: ""
    val title = root["title"].strOrNull() ?: "Unknown"

    // Duration is seconds as number; guard for bad types
    val duration = root["duration"].doubleOrNull()?.let { java.time.Duration.ofSeconds(it.toLong()) }

    // Nullable simple primitives
    val description = root["description"].strOrNull()
    val uploader = root["uploader"].strOrNull() ?: root["channel"].strOrNull()
    val uploaderUrl = root["uploader_url"].strOrNull() ?: root["channel_url"].strOrNull()
    val uploadDate = root["upload_date"].strOrNull()
    val viewCount = root["view_count"].longOrNull()
    val likeCount = root["like_count"].longOrNull()
    val width = root["width"].intOrNull()
    val height = root["height"].intOrNull()
    val fps = root["fps"].doubleOrNull()
    val formatNote = root["format_note"].strOrNull()
    val thumbnail = root["thumbnail"].strOrNull()

    // Tags / categories: accept only string primitives, ignore others/null
    val tags = (root["tags"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList()
    val categories = (root["categories"].arrOrNull()?.mapNotNull { it.strOrNull() }) ?: emptyList()

    // Subtitles: handle null, non-array per-language values, and incomplete entries
    val availableSubtitles: Map<String, SubtitleInfo> = buildMap {
        val subsObj = root["subtitles"].objOrNull()
        if (subsObj != null) {
            for ((lang, dataEl) in subsObj) {
                val arr = dataEl.arrOrNull() ?: continue
                val formats = arr.mapNotNull { fe ->
                    val fo = (fe as? JsonObject) ?: return@mapNotNull null
                    val ext = fo["ext"].strOrNull() ?: return@mapNotNull null
                    SubtitleFormat(
                        ext = ext,
                        url = fo["url"].strOrNull(),
                        name = fo["name"].strOrNull()
                    )
                }
                if (formats.isNotEmpty()) {
                    put(
                        lang,
                        SubtitleInfo(
                            language = lang,
                            languageName = arr.firstOrNull()
                                ?.objOrNull()
                                ?.get("name")
                                ?.strOrNull(),
                            formats = formats,
                            autoGenerated = false
                        )
                    )
                }
            }
        }
    }

    // Chapters: accept only well-formed objects with numeric start/end
    val chapters: List<ChapterInfo> = buildList {
        val arr = root["chapters"].arrOrNull() ?: emptyList()
        for (el in arr) {
            val o = el.objOrNull() ?: continue
            val start = o["start_time"].doubleOrNull() ?: continue
            val end = o["end_time"].doubleOrNull() ?: continue
            add(
                ChapterInfo(
                    title = o["title"].strOrNull(),
                    startTime = start,
                    endTime = end
                )
            )
        }
    }

    return VideoInfo(
        id = id,
        title = title,
        url = url,
        thumbnail = thumbnail,
        duration = duration,
        description = description,
        uploader = uploader,
        uploaderUrl = uploaderUrl,
        uploadDate = uploadDate,
        viewCount = viewCount,
        likeCount = likeCount,
        width = width,
        height = height,
        fps = fps,
        formatNote = formatNote,
        availableSubtitles = availableSubtitles,
        chapters = chapters,
        tags = tags,
        categories = categories
    )
}


// --- Fully safe: parse a playlist JSON blob (with entries as embedded video JSON) ---
private fun parsePlaylistInfoFromJson(jsonString: String): PlaylistInfo {
    fun JsonElement?.objOrNull() = this as? JsonObject
    fun JsonElement?.arrOrNull() = this as? JsonArray
    fun JsonElement?.strOrNull() = this?.jsonPrimitive?.contentOrNull
    fun JsonElement?.intOrNull() = this?.jsonPrimitive?.intOrNull

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val root = try {
        json.parseToJsonElement(jsonString).objOrNull() ?: kotlinx.serialization.json.buildJsonObject { }
    } catch (_: Exception) {
        kotlinx.serialization.json.buildJsonObject { }
    }

    // Entries can be null, mixed, or partially malformed; parse defensively
    val entries: List<VideoInfo> =
        root["entries"].arrOrNull()
            ?.mapNotNull { el ->
                val eo = el.objOrNull() ?: return@mapNotNull null
                // Reuse the robust parser above
                try { parseVideoInfoFromJson(eo.toString()) } catch (_: Exception) { null }
            }
            ?: emptyList()

    // Count: prefer playlist_count, otherwise entries.size
    val entryCount = root["playlist_count"].intOrNull() ?: entries.size

    return PlaylistInfo(
        id = root["id"].strOrNull(),
        title = root["title"].strOrNull(),
        description = root["description"].strOrNull(),
        uploader = root["uploader"].strOrNull(),
        uploaderUrl = root["uploader_url"].strOrNull(),
        entries = entries,
        entryCount = entryCount
    )
}

private fun parseSubtitles(subtitlesElement: JsonElement?): Map<String, SubtitleInfo> {
    val obj = subtitlesElement as? JsonObject ?: return emptyMap()

    return buildMap {
        for ((lang, data) in obj) {
            val arr = data as? JsonArray ?: continue // saute si null / pas un array
            val formats = arr.mapNotNull { fe ->
                val fo = fe as? JsonObject ?: return@mapNotNull null
                val ext = fo["ext"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                SubtitleFormat(
                    ext = ext,
                    url = fo["url"]?.jsonPrimitive?.contentOrNull,
                    name = fo["name"]?.jsonPrimitive?.contentOrNull
                )
            }
            if (formats.isNotEmpty()) {
                put(
                    lang,
                    SubtitleInfo(
                        language = lang,
                        languageName = arr.firstOrNull()
                            ?.jsonObject
                            ?.get("name")
                            ?.jsonPrimitive
                            ?.contentOrNull,
                        formats = formats,
                        autoGenerated = false
                    )
                )
            }
        }
    }
}


private fun parseChapters(chaptersElement: JsonElement?): List<ChapterInfo> {
    val chaptersArray = chaptersElement as? JsonArray ?: return emptyList()
    return chaptersArray.mapNotNull { chapterElement ->
        try {
            val chapterObj = chapterElement as? JsonObject ?: return@mapNotNull null
            ChapterInfo(
                title = chapterObj["title"]?.jsonPrimitive?.contentOrNull,
                startTime = chapterObj["start_time"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                endTime = chapterObj["end_time"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            )
        } catch (_: Exception) { null }
    }
}
