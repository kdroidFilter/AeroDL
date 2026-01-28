package io.github.kdroidfilter.ffmpeg.util

import io.github.kdroidfilter.ffmpeg.core.*
import io.github.kdroidfilter.ffmpeg.model.*
import java.io.File

/**
 * Builds FFmpeg command line arguments from conversion options.
 */
object CommandBuilder {

    // Cache for detected hardware encoder
    private var detectedH264Encoder: String? = null
    private var detectedHevcEncoder: String? = null

    /**
     * Build the complete FFmpeg command.
     *
     * @param ffmpegPath Path to FFmpeg binary.
     * @param inputFile Input media file.
     * @param outputFile Output file path.
     * @param options Conversion options.
     * @return List of command arguments.
     */
    fun build(
        ffmpegPath: String,
        inputFile: File,
        outputFile: File,
        options: ConversionOptions
    ): List<String> = buildList {
        add(ffmpegPath)

        // Log level
        add("-loglevel")
        add(options.logLevel.value)

        // Enable progress stats
        add("-stats")

        // Determine encoder to use (hardware or software)
        val resolvedEncoder = if (options.useHardwareAcceleration && options.video != null) {
            resolveHardwareEncoder(ffmpegPath, options.video.encoder)
        } else {
            options.video?.encoder?.ffmpegName
        }

        // Hardware acceleration input option
        options.hwAccel?.let {
            add("-hwaccel")
            add(it)
        }

        // VA-API device for Linux
        if (resolvedEncoder != null) {
            val extraArgs = HardwareAcceleration.getEncoderExtraArgs(resolvedEncoder)
            addAll(extraArgs)
        }

        // Input time trimming (before input for efficiency)
        options.startTime?.let {
            add("-ss")
            add(formatDuration(it))
        }

        // Input file
        add("-i")
        add(inputFile.absolutePath)

        // Duration/end time
        options.duration?.let {
            add("-t")
            add(formatDuration(it))
        }
        options.endTime?.let {
            if (options.duration == null) {
                add("-to")
                add(formatDuration(it))
            }
        }

        // Stream mapping
        addStreamMapping(this, options.streamSelection)

        // Video encoding options
        if (options.video != null) {
            addVideoOptions(this, options.video, resolvedEncoder)
        } else {
            add("-vn") // No video
        }

        // Audio encoding options
        if (options.audio != null) {
            addAudioOptions(this, options.audio)
        } else {
            add("-an") // No audio
        }

        // Subtitle options
        if (options.subtitle != null) {
            addSubtitleOptions(this, options.subtitle)
        } else {
            add("-sn") // No subtitles
        }

        // Metadata mapping
        options.mapMetadata?.let {
            add("-map_metadata")
            add(it.toString())
        }

        // Chapter mapping
        options.mapChapters?.let {
            add("-map_chapters")
            add(it.toString())
        }

        // Custom metadata
        options.metadata.forEach { (key, value) ->
            add("-metadata")
            add("$key=$value")
        }

        // Output format
        options.outputFormat?.let {
            add("-f")
            add(it)
        }

        // Overwrite
        if (options.overwrite) {
            add("-y")
        }

        // Extra arguments
        addAll(options.extraArgs)

        // Output file
        add(outputFile.absolutePath)
    }

    /**
     * Resolves the best encoder to use, preferring hardware acceleration.
     */
    private fun resolveHardwareEncoder(ffmpegPath: String, encoder: VideoEncoder): String {
        return when (encoder) {
            VideoEncoder.H264 -> {
                if (detectedH264Encoder == null) {
                    detectedH264Encoder = HardwareAcceleration.getBestH264Encoder(ffmpegPath)
                }
                detectedH264Encoder!!
            }
            VideoEncoder.H265 -> {
                if (detectedHevcEncoder == null) {
                    detectedHevcEncoder = HardwareAcceleration.getBestHevcEncoder(ffmpegPath)
                }
                detectedHevcEncoder!!
            }
            else -> encoder.ffmpegName
        }
    }

    private fun addStreamMapping(cmd: MutableList<String>, selection: StreamSelection) {
        // Video stream
        when (selection.videoStream) {
            null -> {
                cmd.add("-map")
                cmd.add("0:v:0?")
            }
            -1 -> { /* Excluded via -vn */ }
            else -> {
                cmd.add("-map")
                cmd.add("0:v:${selection.videoStream}")
            }
        }

        // Audio stream
        when (selection.audioStream) {
            null -> {
                cmd.add("-map")
                cmd.add("0:a:0?")
            }
            -1 -> { /* Excluded via -an */ }
            else -> {
                cmd.add("-map")
                cmd.add("0:a:${selection.audioStream}")
            }
        }

        // Subtitle stream
        when (selection.subtitleStream) {
            null -> {
                cmd.add("-map")
                cmd.add("0:s:0?")
            }
            -1 -> { /* Excluded via -sn */ }
            else -> {
                cmd.add("-map")
                cmd.add("0:s:${selection.subtitleStream}")
            }
        }
    }

    private fun addVideoOptions(cmd: MutableList<String>, video: VideoOptions, resolvedEncoder: String? = null) {
        val encoderName = resolvedEncoder ?: video.encoder.ffmpegName
        val isHwEncoder = HardwareAcceleration.isHardwareEncoder(encoderName)

        when (video.compressionType) {
            CompressionType.COPY -> {
                cmd.add("-c:v")
                cmd.add("copy")
                return
            }
            CompressionType.CRF -> {
                cmd.add("-c:v")
                cmd.add(encoderName)

                // Hardware encoders use different quality parameters
                if (isHwEncoder) {
                    // NVENC uses -cq for constant quality, QSV uses -global_quality
                    when {
                        encoderName.contains("nvenc") -> {
                            val cq = video.crf ?: video.encoder.defaultCrf
                            cmd.add("-cq")
                            cmd.add(cq.coerceIn(0, 51).toString())
                            cmd.add("-preset")
                            cmd.add("p4") // NVENC preset (p1-p7, p4 is balanced)
                        }
                        encoderName.contains("qsv") -> {
                            val quality = video.crf ?: video.encoder.defaultCrf
                            cmd.add("-global_quality")
                            cmd.add(quality.coerceIn(1, 51).toString())
                        }
                        encoderName.contains("amf") -> {
                            val quality = video.crf ?: video.encoder.defaultCrf
                            cmd.add("-quality")
                            cmd.add("quality") // AMF quality mode
                            cmd.add("-rc")
                            cmd.add("cqp")
                            cmd.add("-qp_i")
                            cmd.add(quality.coerceIn(0, 51).toString())
                            cmd.add("-qp_p")
                            cmd.add(quality.coerceIn(0, 51).toString())
                        }
                        encoderName.contains("videotoolbox") -> {
                            // VideoToolbox uses -q:v for quality (1-100, higher is better)
                            val crf = video.crf ?: video.encoder.defaultCrf
                            val quality = (100 - (crf * 2)).coerceIn(1, 100)
                            cmd.add("-q:v")
                            cmd.add(quality.toString())
                        }
                        encoderName.contains("vaapi") -> {
                            val quality = video.crf ?: video.encoder.defaultCrf
                            cmd.add("-qp")
                            cmd.add(quality.coerceIn(0, 51).toString())
                        }
                    }
                } else {
                    // Software encoder - use standard CRF
                    val crf = video.crf ?: video.encoder.defaultCrf
                    cmd.add("-crf")
                    cmd.add(crf.coerceIn(video.encoder.minCrf, video.encoder.maxCrf).toString())
                }
            }
            CompressionType.CBR -> {
                cmd.add("-c:v")
                cmd.add(encoderName)

                video.bitrate?.let {
                    cmd.add("-b:v")
                    cmd.add(it.ffmpegValue)
                }
            }
        }

        // Preset - only for software encoders (HW encoders have their own preset handling above)
        if (!isHwEncoder && video.encoder.supportsPreset) {
            val preset = video.preset ?: video.encoder.defaultPreset
            cmd.add("-preset")
            cmd.add(preset.ffmpegValue)
        }

        // Profile - skip for some hardware encoders that auto-detect
        if (!isHwEncoder) {
            val profile = video.profile ?: video.encoder.getProfile(video.pixelFormat)
            profile?.let {
                cmd.add("-profile:v")
                cmd.add(it)
            }
        }

        // Pixel format
        video.pixelFormat?.let {
            cmd.add("-pix_fmt")
            cmd.add(it.ffmpegValue)
        }

        // Frame rate
        video.frameRate?.let {
            cmd.add("-r")
            cmd.add(it.toString())
        }

        // Resolution
        video.resolution?.let {
            cmd.add("-vf")
            cmd.add(it.toScaleFilter())
        }

        // Custom filters (append to existing -vf if present)
        if (video.filters.isNotEmpty()) {
            val filterString = video.filters.joinToString(",")
            // Check if we already have a -vf flag (from resolution)
            val vfIndex = cmd.lastIndexOf("-vf")
            if (vfIndex >= 0 && vfIndex < cmd.size - 1) {
                cmd[vfIndex + 1] = "${cmd[vfIndex + 1]},$filterString"
            } else {
                cmd.add("-vf")
                cmd.add(filterString)
            }
        }
    }

    private fun addAudioOptions(cmd: MutableList<String>, audio: AudioOptions) {
        when (audio.compressionType) {
            AudioCompressionType.COPY -> {
                cmd.add("-c:a")
                cmd.add("copy")
                return
            }
            AudioCompressionType.CBR -> {
                cmd.add("-c:a")
                cmd.add(audio.encoder.ffmpegName)

                audio.bitrate?.let {
                    cmd.add("-b:a")
                    cmd.add(it.ffmpegValue)
                }
            }
            AudioCompressionType.VBR -> {
                cmd.add("-c:a")
                cmd.add(audio.encoder.ffmpegName)

                val vbr = audio.vbr ?: audio.encoder.defaultVbr
                vbr?.let {
                    // Different encoders use different VBR flags
                    when (audio.encoder) {
                        AudioEncoder.AAC_FDK -> {
                            cmd.add("-vbr")
                            cmd.add(it.toString())
                        }
                        AudioEncoder.MP3 -> {
                            cmd.add("-q:a")
                            cmd.add(it.toString())
                        }
                        AudioEncoder.VORBIS -> {
                            cmd.add("-q:a")
                            cmd.add(it.toString())
                        }
                        else -> {}
                    }
                }
            }
        }

        // Sample rate
        audio.sampleRate?.let {
            cmd.add("-ar")
            cmd.add(it.ffmpegValue)
        }

        // Channels
        audio.channels?.let {
            cmd.add("-ac")
            cmd.add(it.ffmpegValue)

            // Opus needs mapping_family for surround
            if (audio.encoder == AudioEncoder.OPUS && it.count > 2) {
                cmd.add("-mapping_family")
                cmd.add("1")
            }
        }

        // Audio filters
        if (audio.filters.isNotEmpty()) {
            cmd.add("-af")
            cmd.add(audio.filters.joinToString(","))
        }
    }

    private fun addSubtitleOptions(cmd: MutableList<String>, subtitle: SubtitleOptions) {
        if (subtitle.copy) {
            cmd.add("-c:s")
            cmd.add("copy")
        } else {
            subtitle.encoder?.let {
                cmd.add("-c:s")
                cmd.add(it)
            }
        }
    }

    /**
     * Build FFprobe command for media analysis.
     */
    fun buildProbeCommand(
        ffprobePath: String,
        inputFile: File
    ): List<String> = listOf(
        ffprobePath,
        "-v", "quiet",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        inputFile.absolutePath
    )

    private fun formatDuration(duration: java.time.Duration): String {
        val hours = duration.toHours()
        val minutes = (duration.toMinutes() % 60)
        val seconds = (duration.seconds % 60)
        val millis = (duration.toMillis() % 1000)
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}

/**
 * Extension function to add a parameter only if the value is not null.
 */
internal fun MutableList<String>.addCmd(param: String, value: String?) {
    value?.let {
        add(param)
        add(it)
    }
}
