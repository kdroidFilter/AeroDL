package io.github.kdroidfilter.ffmpeg.model

/**
 * Common video resolutions.
 */
enum class Resolution(
    val width: Int,
    val height: Int,
    val displayName: String
) {
    P360(640, 360, "360p"),
    P480(854, 480, "480p (SD)"),
    P720(1280, 720, "720p (HD)"),
    P1080(1920, 1080, "1080p (Full HD)"),
    P1440(2560, 1440, "1440p (2K)"),
    P2160(3840, 2160, "2160p (4K)"),
    P4320(7680, 4320, "4320p (8K)");

    /**
     * Generate FFmpeg scale filter string that preserves aspect ratio.
     * Uses -2 for the variable dimension to ensure divisibility by 2.
     */
    fun toScaleFilter(preserveAspectRatio: Boolean = true): String =
        if (preserveAspectRatio) "scale=-2:$height" else "scale=$width:$height"

    companion object {
        fun fromHeight(height: Int): Resolution? =
            entries.minByOrNull { kotlin.math.abs(it.height - height) }
    }
}

/**
 * Pixel formats for video encoding.
 */
enum class PixelFormat(
    val ffmpegValue: String,
    val displayName: String,
    val bitDepth: Int
) {
    BIT_8("yuv420p", "8-bit (SDR)", 8),
    BIT_10("yuv420p10le", "10-bit (HDR compatible)", 10);

    companion object {
        fun fromBitDepth(depth: Int): PixelFormat =
            if (depth >= 10) BIT_10 else BIT_8

        fun fromFfmpegValue(value: String): PixelFormat? = when {
            value.contains("10") -> BIT_10
            else -> BIT_8
        }
    }
}

/**
 * Audio sample rates.
 */
enum class SampleRate(val hz: Int, val displayName: String) {
    HZ_22050(22050, "22.05 kHz"),
    HZ_44100(44100, "44.1 kHz (CD quality)"),
    HZ_48000(48000, "48 kHz (DVD/Broadcast)"),
    HZ_96000(96000, "96 kHz (Hi-Res)"),
    HZ_192000(192000, "192 kHz (Hi-Res)");

    val ffmpegValue: String get() = hz.toString()

    companion object {
        fun fromHz(hz: Int): SampleRate? =
            entries.minByOrNull { kotlin.math.abs(it.hz - hz) }
    }
}

/**
 * Audio channel configurations.
 */
enum class Channels(val count: Int, val displayName: String) {
    MONO(1, "Mono"),
    STEREO(2, "Stereo"),
    SURROUND_5_1(6, "5.1 Surround"),
    SURROUND_7_1(8, "7.1 Surround");

    val ffmpegValue: String get() = count.toString()

    companion object {
        fun fromCount(count: Int): Channels? = when (count) {
            1 -> MONO
            2 -> STEREO
            6 -> SURROUND_5_1
            8 -> SURROUND_7_1
            else -> null
        }
    }
}

/**
 * Common container formats.
 */
enum class ContainerFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val supportsVideo: Boolean = true,
    val supportsAudio: Boolean = true,
    val supportsSubtitles: Boolean = false
) {
    MP4("mp4", "video/mp4", "MP4", supportsSubtitles = true),
    MKV("mkv", "video/x-matroska", "Matroska (MKV)", supportsSubtitles = true),
    WEBM("webm", "video/webm", "WebM"),
    AVI("avi", "video/x-msvideo", "AVI"),
    MOV("mov", "video/quicktime", "QuickTime (MOV)", supportsSubtitles = true),
    FLV("flv", "video/x-flv", "Flash Video (FLV)"),
    TS("ts", "video/mp2t", "MPEG Transport Stream"),
    MP3("mp3", "audio/mpeg", "MP3", supportsVideo = false),
    AAC("aac", "audio/aac", "AAC", supportsVideo = false),
    M4A("m4a", "audio/mp4", "M4A (AAC)", supportsVideo = false),
    OGG("ogg", "audio/ogg", "Ogg", supportsVideo = false),
    OPUS("opus", "audio/opus", "Opus", supportsVideo = false),
    FLAC("flac", "audio/flac", "FLAC", supportsVideo = false),
    WAV("wav", "audio/wav", "WAV", supportsVideo = false),
    SRT("srt", "application/x-subrip", "SubRip", supportsVideo = false, supportsAudio = false, supportsSubtitles = true),
    ASS("ass", "text/x-ass", "Advanced SubStation Alpha", supportsVideo = false, supportsAudio = false, supportsSubtitles = true),
    VTT("vtt", "text/vtt", "WebVTT", supportsVideo = false, supportsAudio = false, supportsSubtitles = true);

    companion object {
        fun fromExtension(ext: String): ContainerFormat? =
            entries.find { it.extension.equals(ext.removePrefix("."), ignoreCase = true) }

        val videoFormats = entries.filter { it.supportsVideo }
        val audioFormats = entries.filter { it.supportsAudio && !it.supportsVideo }
    }
}
