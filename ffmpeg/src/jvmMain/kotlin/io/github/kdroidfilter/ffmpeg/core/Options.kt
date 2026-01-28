package io.github.kdroidfilter.ffmpeg.core

import io.github.kdroidfilter.ffmpeg.model.*
import java.time.Duration

/**
 * Configuration options for video encoding.
 */
data class VideoOptions(
    val encoder: VideoEncoder = VideoEncoder.H264,
    val compressionType: CompressionType = CompressionType.CRF,
    val crf: Int? = null,
    val bitrate: VideoBitrate? = null,
    val preset: EncoderPreset? = null,
    val resolution: Resolution? = null,
    val pixelFormat: PixelFormat? = null,
    val frameRate: Double? = null,
    val profile: String? = null,
    val filters: List<String> = emptyList()
)

/**
 * Configuration options for audio encoding.
 */
data class AudioOptions(
    val encoder: AudioEncoder = AudioEncoder.AAC,
    val compressionType: AudioCompressionType = AudioCompressionType.CBR,
    val bitrate: AudioBitrate? = null,
    val vbr: Int? = null,
    val sampleRate: SampleRate? = null,
    val channels: Channels? = null,
    val filters: List<String> = emptyList()
)

/**
 * Configuration options for subtitle handling.
 */
data class SubtitleOptions(
    val copy: Boolean = true,
    val encoder: String? = null
)

/**
 * Stream selection configuration.
 * - `null` = use first available stream
 * - `-1` = exclude stream
 * - `0+` = use specific stream index
 */
data class StreamSelection(
    val videoStream: Int? = null,
    val audioStream: Int? = null,
    val subtitleStream: Int? = -1
)

/**
 * Main configuration options for an FFmpeg conversion.
 *
 * @property video Video encoding options. Null to exclude video.
 * @property audio Audio encoding options. Null to exclude audio.
 * @property subtitle Subtitle handling options. Null to exclude subtitles.
 * @property streamSelection Which streams to select from the input.
 * @property outputFormat Force output format (e.g., "mp4", "mkv", "mp3").
 * @property startTime Start time for trimming. Null for no trim.
 * @property duration Duration to encode. Null for full length.
 * @property endTime End time for trimming. Null for no trim.
 * @property overwrite Overwrite output file if it exists.
 * @property metadata Metadata to set on output file.
 * @property mapMetadata Map metadata from input (-1 to disable).
 * @property mapChapters Map chapters from input (-1 to disable).
 * @property extraArgs Additional FFmpeg arguments.
 * @property timeout Maximum duration for the conversion. Null for no timeout.
 * @property logLevel FFmpeg log level.
 * @property twoPass Enable two-pass encoding for better quality at target bitrate.
 * @property hwAccel Hardware acceleration (e.g., "cuda", "vaapi", "videotoolbox").
 * @property useHardwareAcceleration Auto-detect and use hardware encoding if available (default: true).
 */
data class ConversionOptions(
    val video: VideoOptions? = VideoOptions(),
    val audio: AudioOptions? = AudioOptions(),
    val subtitle: SubtitleOptions? = null,
    val streamSelection: StreamSelection = StreamSelection(),
    val outputFormat: String? = null,
    val startTime: Duration? = null,
    val duration: Duration? = null,
    val endTime: Duration? = null,
    val overwrite: Boolean = true,
    val metadata: Map<String, String> = emptyMap(),
    val mapMetadata: Int? = 0,
    val mapChapters: Int? = 0,
    val extraArgs: List<String> = emptyList(),
    val timeout: Duration? = Duration.ofHours(4),
    val logLevel: LogLevel = LogLevel.INFO,
    val twoPass: Boolean = false,
    val hwAccel: String? = null,
    val useHardwareAcceleration: Boolean = true
) {
    companion object {
        /** Quick audio extraction to MP3. */
        fun audioMp3(bitrate: AudioBitrate = AudioBitrate.K192) = ConversionOptions(
            video = null,
            audio = AudioOptions(
                encoder = AudioEncoder.MP3,
                bitrate = bitrate
            )
        )

        /** Quick audio extraction to AAC. */
        fun audioAac(bitrate: AudioBitrate = AudioBitrate.K192) = ConversionOptions(
            video = null,
            audio = AudioOptions(
                encoder = AudioEncoder.AAC,
                bitrate = bitrate
            )
        )

        /** Quick video copy (no re-encoding). */
        fun copy() = ConversionOptions(
            video = VideoOptions(compressionType = CompressionType.COPY),
            audio = AudioOptions(compressionType = AudioCompressionType.COPY)
        )

        /** H.264 encoding with CRF quality. */
        fun h264(crf: Int = 23, preset: EncoderPreset = EncoderPreset.MEDIUM) = ConversionOptions(
            video = VideoOptions(
                encoder = VideoEncoder.H264,
                compressionType = CompressionType.CRF,
                crf = crf,
                preset = preset
            )
        )

        /** H.265/HEVC encoding with CRF quality. */
        fun h265(crf: Int = 28, preset: EncoderPreset = EncoderPreset.MEDIUM) = ConversionOptions(
            video = VideoOptions(
                encoder = VideoEncoder.H265,
                compressionType = CompressionType.CRF,
                crf = crf,
                preset = preset
            )
        )

        /** AV1 encoding with CRF quality. */
        fun av1(crf: Int = 35, preset: Int = 6) = ConversionOptions(
            video = VideoOptions(
                encoder = VideoEncoder.AV1,
                compressionType = CompressionType.CRF,
                crf = crf,
                preset = EncoderPreset.fromSvtAv1Preset(preset)
            )
        )
    }
}

/**
 * FFmpeg log level.
 */
enum class LogLevel(val value: String) {
    QUIET("quiet"),
    PANIC("panic"),
    FATAL("fatal"),
    ERROR("error"),
    WARNING("warning"),
    INFO("info"),
    VERBOSE("verbose"),
    DEBUG("debug"),
    TRACE("trace")
}
