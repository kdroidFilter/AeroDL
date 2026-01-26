package io.github.kdroidfilter.ffmpeg.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Duration

/**
 * Complete media information extracted from a file via FFprobe.
 */
data class MediaInfo(
    val file: File,
    val format: FormatInfo,
    val videoStreams: List<VideoStreamInfo>,
    val audioStreams: List<AudioStreamInfo>,
    val subtitleStreams: List<SubtitleStreamInfo>
) {
    /** Primary video stream, if any. */
    val primaryVideo: VideoStreamInfo? get() = videoStreams.firstOrNull()

    /** Primary audio stream, if any. */
    val primaryAudio: AudioStreamInfo? get() = audioStreams.firstOrNull()

    /** Whether the media contains video. */
    val hasVideo: Boolean get() = videoStreams.isNotEmpty()

    /** Whether the media contains audio. */
    val hasAudio: Boolean get() = audioStreams.isNotEmpty()

    /** Whether the media contains subtitles. */
    val hasSubtitles: Boolean get() = subtitleStreams.isNotEmpty()

    /** Media duration. */
    val duration: Duration? get() = format.duration

    /** File size in bytes. */
    val fileSize: Long get() = file.length()
}

/**
 * Format/container information.
 */
data class FormatInfo(
    val formatName: String?,
    val formatLongName: String?,
    val duration: Duration?,
    val bitRate: Long?,
    val streamCount: Int
)

/**
 * Video stream information.
 */
data class VideoStreamInfo(
    val index: Int,
    val codec: String?,
    val codecLongName: String?,
    val profile: String?,
    val width: Int?,
    val height: Int?,
    val frameRate: Double?,
    val bitRate: Long?,
    val pixelFormat: String?,
    val bitDepth: Int?,
    val level: Int?,
    val fieldOrder: String?,
    val aspectRatio: String?,
    val hasFilmGrain: Boolean,
    val title: String?,
    val language: String?
) {
    /** Video resolution (e.g., "1920x1080"). */
    val resolution: String? get() = if (width != null && height != null) "${width}x${height}" else null

    /** Detect if video is HDR based on pixel format. */
    val isHdr: Boolean get() = (bitDepth ?: 8) >= 10 || pixelFormat?.contains("10") == true
}

/**
 * Audio stream information.
 */
data class AudioStreamInfo(
    val index: Int,
    val codec: String?,
    val codecLongName: String?,
    val profile: String?,
    val sampleRate: Int?,
    val channels: Int?,
    val channelLayout: String?,
    val bitRate: Long?,
    val bitDepth: Int?,
    val title: String?,
    val language: String?
)

/**
 * Subtitle stream information.
 */
data class SubtitleStreamInfo(
    val index: Int,
    val codec: String?,
    val codecLongName: String?,
    val title: String?,
    val language: String?
)

// --- FFprobe JSON response models ---

@Serializable
internal data class FfprobeResponse(
    val format: FfprobeFormat? = null,
    val streams: List<FfprobeStream> = emptyList()
)

@Serializable
internal data class FfprobeFormat(
    @SerialName("format_name") val formatName: String? = null,
    @SerialName("format_long_name") val formatLongName: String? = null,
    val duration: String? = null,
    @SerialName("bit_rate") val bitRate: String? = null,
    @SerialName("nb_streams") val nbStreams: Int? = null
)

@Serializable
internal data class FfprobeStream(
    val index: Int = 0,
    @SerialName("codec_type") val codecType: String? = null,
    @SerialName("codec_name") val codecName: String? = null,
    @SerialName("codec_long_name") val codecLongName: String? = null,
    val profile: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("pix_fmt") val pixFmt: String? = null,
    @SerialName("r_frame_rate") val rFrameRate: String? = null,
    @SerialName("avg_frame_rate") val avgFrameRate: String? = null,
    @SerialName("bit_rate") val bitRate: String? = null,
    @SerialName("bits_per_raw_sample") val bitsPerRawSample: String? = null,
    @SerialName("bits_per_sample") val bitsPerSample: Int? = null,
    val level: Int? = null,
    @SerialName("field_order") val fieldOrder: String? = null,
    @SerialName("display_aspect_ratio") val displayAspectRatio: String? = null,
    @SerialName("sample_rate") val sampleRate: String? = null,
    val channels: Int? = null,
    @SerialName("channel_layout") val channelLayout: String? = null,
    @SerialName("film_grain") val filmGrain: Int? = null,
    val tags: FfprobeStreamTags? = null
)

@Serializable
internal data class FfprobeStreamTags(
    val title: String? = null,
    val language: String? = null
)
