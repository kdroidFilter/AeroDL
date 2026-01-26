package io.github.kdroidfilter.ffmpeg

import io.github.kdroidfilter.ffmpeg.core.*
import io.github.kdroidfilter.ffmpeg.model.*
import io.github.kdroidfilter.ffmpeg.util.*
import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Main entry point for FFmpeg operations.
 *
 * Provides async, event-driven API for:
 * - Media conversion (video/audio transcoding)
 * - Media analysis (via FFprobe)
 * - Binary management (auto-download, updates)
 *
 * Example usage:
 * ```kotlin
 * val ffmpeg = FfmpegWrapper()
 * ffmpeg.initialize { event ->
 *     when (event) {
 *         is InitEvent.Completed -> println("Ready!")
 *         else -> {}
 *     }
 * }
 *
 * val handle = ffmpeg.convert(
 *     inputFile = File("input.mkv"),
 *     outputFile = File("output.mp4"),
 *     options = ConversionOptions.h264(crf = 23)
 * ) { event ->
 *     when (event) {
 *         is ConversionEvent.Progress -> println("Progress: ${event.timeProcessed}")
 *         is ConversionEvent.Completed -> println("Done: ${event.outputFile}")
 *         is ConversionEvent.Error -> println("Error: ${event.message}")
 *         else -> {}
 *     }
 * }
 * ```
 */
class FfmpegWrapper {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- Configurable Properties ---

    /** Path to FFmpeg binary. Auto-detected if not set. */
    @Volatile
    var ffmpegPath: String = PlatformUtils.getDefaultFfmpegPath()

    /** Path to FFprobe binary. Auto-detected if not set. */
    @Volatile
    var ffprobePath: String = PlatformUtils.getDefaultFfprobePath()

    // --- Caches ---

    @Volatile
    private var cachedFfmpegVersion: String? = null

    @Volatile
    private var cachedFfprobeVersion: String? = null

    private val mediaInfoCache = ConcurrentHashMap<String, MediaInfo>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --- Initialization ---

    /**
     * Initialize FFmpeg, downloading if necessary.
     * Emits [InitEvent]s to track progress.
     */
    fun initialize(onEvent: (InitEvent) -> Unit): Job = scope.launch {
        try {
            onEvent(InitEvent.CheckingFfmpeg)

            // Check if FFmpeg is available
            var ffmpegAvailable = isAvailable()

            if (!ffmpegAvailable) {
                // Try system PATH
                val systemFfmpeg = PlatformUtils.findFfmpegInSystemPath()
                if (systemFfmpeg != null) {
                    ffmpegPath = systemFfmpeg
                    val systemFfprobe = PlatformUtils.findFfprobeInSystemPath()
                    if (systemFfprobe != null) {
                        ffprobePath = systemFfprobe
                    }
                    ffmpegAvailable = true
                }
            }

            if (!ffmpegAvailable) {
                // Download FFmpeg
                onEvent(InitEvent.DownloadingFfmpeg)

                val assetPattern = PlatformUtils.getFfmpegAssetPatternForSystem()
                    ?: error("Unsupported platform for FFmpeg download")

                val ffmpegFetcher = GitHubReleaseFetcher("yt-dlp", "FFmpeg-Builds")

                val installedPath = PlatformUtils.downloadAndInstallFfmpeg(
                    assetPattern = assetPattern,
                    forceDownload = false,
                    ffmpegFetcher = ffmpegFetcher
                ) { bytesRead, totalBytes ->
                    val percent = totalBytes?.let { bytesRead.toDouble() / it * 100 }
                    onEvent(InitEvent.FfmpegProgress(bytesRead, totalBytes, percent))
                }

                if (installedPath == null) {
                    onEvent(InitEvent.Error("Failed to download FFmpeg"))
                    return@launch
                }

                ffmpegPath = installedPath
                ffprobePath = PlatformUtils.getDefaultFfprobePath()
            }

            cachedFfmpegVersion = null
            cachedFfprobeVersion = null

            onEvent(InitEvent.Completed(success = true))
        } catch (e: Exception) {
            errorln { "FFmpeg initialization failed: ${e.message}" }
            onEvent(InitEvent.Error(e.message ?: "Unknown error", e))
        }
    }

    /**
     * Initialize in a specific coroutine scope.
     */
    fun initializeIn(externalScope: CoroutineScope, onEvent: (InitEvent) -> Unit): Job =
        externalScope.launch { initialize(onEvent).join() }

    // --- Availability & Version ---

    /**
     * Check if FFmpeg is available and runnable.
     */
    suspend fun isAvailable(): Boolean {
        return PlatformUtils.getFfmpegVersion(ffmpegPath) != null
    }

    /**
     * Check if FFprobe is available and runnable.
     */
    suspend fun isFfprobeAvailable(): Boolean {
        return PlatformUtils.getFfprobeVersion(ffprobePath) != null
    }

    /**
     * Get FFmpeg version string (cached).
     */
    suspend fun version(): String? {
        cachedFfmpegVersion?.let { return it }
        return PlatformUtils.getFfmpegVersion(ffmpegPath).also { cachedFfmpegVersion = it }
    }

    /**
     * Get FFprobe version string (cached).
     */
    suspend fun ffprobeVersion(): String? {
        cachedFfprobeVersion?.let { return it }
        return PlatformUtils.getFfprobeVersion(ffprobePath).also { cachedFfprobeVersion = it }
    }

    // --- Media Analysis ---

    /**
     * Analyze a media file using FFprobe.
     *
     * @param file The media file to analyze.
     * @param useCache Whether to use cached results if available.
     * @return [MediaInfo] containing all stream and format information.
     */
    suspend fun analyze(file: File, useCache: Boolean = true): Result<MediaInfo> = withContext(Dispatchers.IO) {
        val cacheKey = file.absolutePath

        if (useCache) {
            mediaInfoCache[cacheKey]?.let { return@withContext Result.success(it) }
        }

        try {
            val cmd = CommandBuilder.buildProbeCommand(ffprobePath, file)
            debugln { "FFprobe command: ${cmd.joinToString(" ")}" }

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                return@withContext Result.failure(Exception("FFprobe failed: $error"))
            }

            val response = json.decodeFromString<FfprobeResponse>(output)
            val mediaInfo = parseMediaInfo(file, response)

            if (useCache) {
                mediaInfoCache[cacheKey] = mediaInfo
            }

            Result.success(mediaInfo)
        } catch (e: Exception) {
            errorln { "FFprobe analysis failed: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Analyze a media file and emit events.
     */
    fun analyzeAsync(file: File, onEvent: (ProbeEvent) -> Unit): Job = scope.launch {
        analyze(file).fold(
            onSuccess = { onEvent(ProbeEvent.Completed(it)) },
            onFailure = { onEvent(ProbeEvent.Error(it.message ?: "Analysis failed", it)) }
        )
    }

    // --- Conversion ---

    /**
     * Convert a media file.
     *
     * @param inputFile Input media file.
     * @param outputFile Output file path.
     * @param options Conversion options.
     * @param onEvent Callback for conversion events.
     * @return [ConversionHandle] for cancellation.
     */
    fun convert(
        inputFile: File,
        outputFile: File,
        options: ConversionOptions = ConversionOptions(),
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle {
        val job = scope.launch {
            try {
                // Validate input
                if (!inputFile.exists()) {
                    onEvent(ConversionEvent.Error("Input file does not exist: ${inputFile.absolutePath}"))
                    return@launch
                }

                // Get input duration for progress calculation
                val inputInfo = analyze(inputFile).getOrNull()
                val totalDuration = inputInfo?.duration

                // Build command
                val cmd = CommandBuilder.build(ffmpegPath, inputFile, outputFile, options)
                debugln { "FFmpeg command: ${cmd.joinToString(" ")}" }

                // Prepare output directory
                outputFile.parentFile?.mkdirs()

                // Delete existing output if overwrite is enabled
                if (options.overwrite && outputFile.exists()) {
                    outputFile.delete()
                }

                onEvent(ConversionEvent.Started)

                // Start process
                val process = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()

                // Store process for potential cancellation
                val processRef = process

                // Read output
                val outputLines = mutableListOf<String>()
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (!isActive) {
                            processRef.destroyForcibly()
                            return@forEach
                        }

                        outputLines.add(line)
                        if (outputLines.size > 100) outputLines.removeAt(0)

                        // Parse progress
                        val progress = ProgressParser.parse(line)
                        if (progress != null) {
                            onEvent(ConversionEvent.Progress(
                                timeProcessed = progress.time,
                                speed = progress.speed,
                                fps = progress.fps,
                                bitrate = progress.bitrate,
                                rawLine = line
                            ))
                        } else {
                            onEvent(ConversionEvent.Log(line))
                        }
                    }
                }

                // Wait for completion with timeout
                val exitCode = if (options.timeout != null) {
                    withTimeoutOrNull(options.timeout.toMillis()) {
                        withContext(Dispatchers.IO) { process.waitFor() }
                    } ?: run {
                        process.destroyForcibly()
                        onEvent(ConversionEvent.Error("Conversion timed out"))
                        return@launch
                    }
                } else {
                    process.waitFor()
                }

                if (!isActive) {
                    onEvent(ConversionEvent.Cancelled)
                    return@launch
                }

                if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
                    onEvent(ConversionEvent.Completed(outputFile, exitCode))
                } else {
                    val diagnosis = ErrorDiagnostics.diagnose(outputLines)
                    onEvent(ConversionEvent.Error(diagnosis ?: "Conversion failed with exit code $exitCode"))
                }

            } catch (e: CancellationException) {
                onEvent(ConversionEvent.Cancelled)
            } catch (e: Exception) {
                errorln { "Conversion error: ${e.message}" }
                onEvent(ConversionEvent.Error(e.message ?: "Unknown error", e))
            }
        }

        return ConversionHandle(job)
    }

    // --- Quick Conversion Methods ---

    /**
     * Extract audio to MP3.
     */
    fun extractAudioMp3(
        inputFile: File,
        outputFile: File,
        bitrate: AudioBitrate = AudioBitrate.K192,
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle = convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions.audioMp3(bitrate),
        onEvent = onEvent
    )

    /**
     * Extract audio to AAC/M4A.
     */
    fun extractAudioAac(
        inputFile: File,
        outputFile: File,
        bitrate: AudioBitrate = AudioBitrate.K192,
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle = convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions.audioAac(bitrate),
        onEvent = onEvent
    )

    /**
     * Copy streams without re-encoding (remux).
     */
    fun remux(
        inputFile: File,
        outputFile: File,
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle = convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions.copy(),
        onEvent = onEvent
    )

    /**
     * Encode video with H.264 codec.
     */
    fun encodeH264(
        inputFile: File,
        outputFile: File,
        crf: Int = 23,
        preset: EncoderPreset = EncoderPreset.MEDIUM,
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle = convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions.h264(crf, preset),
        onEvent = onEvent
    )

    /**
     * Encode video with H.265/HEVC codec.
     */
    fun encodeH265(
        inputFile: File,
        outputFile: File,
        crf: Int = 28,
        preset: EncoderPreset = EncoderPreset.MEDIUM,
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle = convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions.h265(crf, preset),
        onEvent = onEvent
    )

    /**
     * Trim a video/audio file.
     */
    fun trim(
        inputFile: File,
        outputFile: File,
        startTime: Duration,
        duration: Duration? = null,
        endTime: Duration? = null,
        copyStreams: Boolean = true,
        onEvent: (ConversionEvent) -> Unit
    ): ConversionHandle = convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions(
            video = if (copyStreams) VideoOptions(compressionType = CompressionType.COPY) else VideoOptions(),
            audio = if (copyStreams) AudioOptions(compressionType = AudioCompressionType.COPY) else AudioOptions(),
            startTime = startTime,
            duration = duration,
            endTime = endTime
        ),
        onEvent = onEvent
    )

    // --- Cache Management ---

    /**
     * Clear the media info cache.
     */
    fun clearCache() {
        mediaInfoCache.clear()
        cachedFfmpegVersion = null
        cachedFfprobeVersion = null
    }

    /**
     * Remove a specific file from the cache.
     */
    fun invalidateCache(file: File) {
        mediaInfoCache.remove(file.absolutePath)
    }

    // --- Cleanup ---

    /**
     * Cancel all running operations and clean up resources.
     */
    fun shutdown() {
        scope.cancel()
        clearCache()
    }

    // --- Private Helpers ---

    private fun parseMediaInfo(file: File, response: FfprobeResponse): MediaInfo {
        val format = response.format?.let { fmt ->
            FormatInfo(
                formatName = fmt.formatName,
                formatLongName = fmt.formatLongName,
                duration = fmt.duration?.toDoubleOrNull()?.let { Duration.ofMillis((it * 1000).toLong()) },
                bitRate = fmt.bitRate?.toLongOrNull(),
                streamCount = fmt.nbStreams ?: response.streams.size
            )
        } ?: FormatInfo(null, null, null, null, 0)

        val videoStreams = response.streams
            .filter { it.codecType == "video" }
            .map { stream ->
                VideoStreamInfo(
                    index = stream.index,
                    codec = stream.codecName,
                    codecLongName = stream.codecLongName,
                    profile = stream.profile,
                    width = stream.width,
                    height = stream.height,
                    frameRate = parseFrameRate(stream.rFrameRate ?: stream.avgFrameRate),
                    bitRate = stream.bitRate?.toLongOrNull(),
                    pixelFormat = stream.pixFmt,
                    bitDepth = stream.bitsPerRawSample?.toIntOrNull()
                        ?: stream.pixFmt?.let { if (it.contains("10")) 10 else 8 },
                    level = stream.level,
                    fieldOrder = stream.fieldOrder,
                    aspectRatio = stream.displayAspectRatio,
                    hasFilmGrain = (stream.filmGrain ?: 0) > 0,
                    title = stream.tags?.title,
                    language = stream.tags?.language
                )
            }

        val audioStreams = response.streams
            .filter { it.codecType == "audio" }
            .map { stream ->
                AudioStreamInfo(
                    index = stream.index,
                    codec = stream.codecName,
                    codecLongName = stream.codecLongName,
                    profile = stream.profile,
                    sampleRate = stream.sampleRate?.toIntOrNull(),
                    channels = stream.channels,
                    channelLayout = stream.channelLayout,
                    bitRate = stream.bitRate?.toLongOrNull(),
                    bitDepth = stream.bitsPerSample,
                    title = stream.tags?.title,
                    language = stream.tags?.language
                )
            }

        val subtitleStreams = response.streams
            .filter { it.codecType == "subtitle" }
            .map { stream ->
                SubtitleStreamInfo(
                    index = stream.index,
                    codec = stream.codecName,
                    codecLongName = stream.codecLongName,
                    title = stream.tags?.title,
                    language = stream.tags?.language
                )
            }

        return MediaInfo(
            file = file,
            format = format,
            videoStreams = videoStreams,
            audioStreams = audioStreams,
            subtitleStreams = subtitleStreams
        )
    }

    private fun parseFrameRate(frameRate: String?): Double? {
        if (frameRate == null) return null
        return try {
            if (frameRate.contains("/")) {
                val parts = frameRate.split("/")
                val num = parts[0].toDoubleOrNull() ?: return null
                val den = parts[1].toDoubleOrNull() ?: return null
                if (den == 0.0) null else num / den
            } else {
                frameRate.toDoubleOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }
}
