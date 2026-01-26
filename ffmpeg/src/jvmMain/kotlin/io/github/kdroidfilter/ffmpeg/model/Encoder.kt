package io.github.kdroidfilter.ffmpeg.model

/**
 * Compression type for video encoding.
 */
enum class CompressionType {
    /** Direct stream copy - no re-encoding. */
    COPY,
    /** Constant Bitrate - fixed bitrate throughout. */
    CBR,
    /** Constant Rate Factor - quality-based encoding (recommended). */
    CRF
}

/**
 * Compression type for audio encoding.
 */
enum class AudioCompressionType {
    /** Direct stream copy - no re-encoding. */
    COPY,
    /** Constant Bitrate - fixed bitrate throughout. */
    CBR,
    /** Variable Bitrate - quality-based encoding. */
    VBR
}

/**
 * Video encoders supported by FFmpeg.
 */
enum class VideoEncoder(
    val ffmpegName: String,
    val displayName: String,
    val defaultCrf: Int,
    val minCrf: Int,
    val maxCrf: Int,
    val defaultPreset: EncoderPreset,
    val supportsPreset: Boolean = true
) {
    H264(
        ffmpegName = "libx264",
        displayName = "H.264 (x264)",
        defaultCrf = 23,
        minCrf = 0,
        maxCrf = 51,
        defaultPreset = EncoderPreset.MEDIUM
    ),
    H265(
        ffmpegName = "libx265",
        displayName = "H.265/HEVC (x265)",
        defaultCrf = 28,
        minCrf = 0,
        maxCrf = 51,
        defaultPreset = EncoderPreset.MEDIUM
    ),
    AV1(
        ffmpegName = "libsvtav1",
        displayName = "AV1 (SVT-AV1)",
        defaultCrf = 35,
        minCrf = 0,
        maxCrf = 63,
        defaultPreset = EncoderPreset.PRESET_6
    ),
    VP9(
        ffmpegName = "libvpx-vp9",
        displayName = "VP9",
        defaultCrf = 31,
        minCrf = 0,
        maxCrf = 63,
        defaultPreset = EncoderPreset.GOOD,
        supportsPreset = false
    ),
    MPEG4(
        ffmpegName = "mpeg4",
        displayName = "MPEG-4",
        defaultCrf = 4,
        minCrf = 1,
        maxCrf = 31,
        defaultPreset = EncoderPreset.MEDIUM,
        supportsPreset = false
    );

    /**
     * Get the appropriate video profile for this encoder based on pixel format.
     */
    fun getProfile(pixelFormat: PixelFormat?): String? = when (this) {
        H264 -> if (pixelFormat == PixelFormat.BIT_10) "high10" else "high"
        H265 -> if (pixelFormat == PixelFormat.BIT_10) "main10" else "main"
        AV1 -> "0"
        else -> null
    }
}

/**
 * Audio encoders supported by FFmpeg.
 */
enum class AudioEncoder(
    val ffmpegName: String,
    val displayName: String,
    val supportsCbr: Boolean = true,
    val supportsVbr: Boolean = false,
    val defaultVbr: Int? = null,
    val minVbr: Int? = null,
    val maxVbr: Int? = null
) {
    AAC(
        ffmpegName = "aac",
        displayName = "AAC (Native)"
    ),
    AAC_FDK(
        ffmpegName = "libfdk_aac",
        displayName = "AAC (FDK)",
        supportsVbr = true,
        defaultVbr = 4,
        minVbr = 1,
        maxVbr = 5
    ),
    MP3(
        ffmpegName = "libmp3lame",
        displayName = "MP3 (LAME)",
        supportsVbr = true,
        defaultVbr = 2,
        minVbr = 0,
        maxVbr = 9
    ),
    OPUS(
        ffmpegName = "libopus",
        displayName = "Opus"
    ),
    VORBIS(
        ffmpegName = "libvorbis",
        displayName = "Vorbis",
        supportsVbr = true,
        defaultVbr = 5,
        minVbr = 0,
        maxVbr = 10
    ),
    FLAC(
        ffmpegName = "flac",
        displayName = "FLAC (Lossless)",
        supportsCbr = false
    ),
    AC3(
        ffmpegName = "ac3",
        displayName = "AC3"
    ),
    EAC3(
        ffmpegName = "eac3",
        displayName = "E-AC3"
    ),
    PCM_S16LE(
        ffmpegName = "pcm_s16le",
        displayName = "PCM 16-bit",
        supportsCbr = false
    ),
    PCM_S24LE(
        ffmpegName = "pcm_s24le",
        displayName = "PCM 24-bit",
        supportsCbr = false
    ),
    ALAC(
        ffmpegName = "alac",
        displayName = "ALAC (Apple Lossless)",
        supportsCbr = false
    )
}

/**
 * Encoder presets for speed/quality tradeoff.
 */
enum class EncoderPreset(
    val ffmpegValue: String,
    val x264x265Index: Int,
    val svtAv1Preset: Int?
) {
    ULTRAFAST("ultrafast", 0, null),
    SUPERFAST("superfast", 1, null),
    VERYFAST("veryfast", 2, null),
    FASTER("faster", 3, null),
    FAST("fast", 4, null),
    MEDIUM("medium", 5, null),
    SLOW("slow", 6, null),
    SLOWER("slower", 7, null),
    VERYSLOW("veryslow", 8, null),
    PLACEBO("placebo", 9, null),

    // SVT-AV1 specific presets (0-13, lower = slower/better)
    PRESET_0("0", -1, 0),
    PRESET_1("1", -1, 1),
    PRESET_2("2", -1, 2),
    PRESET_3("3", -1, 3),
    PRESET_4("4", -1, 4),
    PRESET_5("5", -1, 5),
    PRESET_6("6", -1, 6),
    PRESET_7("7", -1, 7),
    PRESET_8("8", -1, 8),
    PRESET_9("9", -1, 9),
    PRESET_10("10", -1, 10),
    PRESET_11("11", -1, 11),
    PRESET_12("12", -1, 12),
    PRESET_13("13", -1, 13),

    // VP9 quality modes
    GOOD("good", -1, null),
    BEST("best", -1, null),
    REALTIME("realtime", -1, null);

    companion object {
        fun fromSvtAv1Preset(preset: Int): EncoderPreset = when (preset.coerceIn(0, 13)) {
            0 -> PRESET_0
            1 -> PRESET_1
            2 -> PRESET_2
            3 -> PRESET_3
            4 -> PRESET_4
            5 -> PRESET_5
            6 -> PRESET_6
            7 -> PRESET_7
            8 -> PRESET_8
            9 -> PRESET_9
            10 -> PRESET_10
            11 -> PRESET_11
            12 -> PRESET_12
            else -> PRESET_13
        }

        /** Standard presets for x264/x265. */
        val x264x265Presets = listOf(
            ULTRAFAST, SUPERFAST, VERYFAST, FASTER, FAST,
            MEDIUM, SLOW, SLOWER, VERYSLOW, PLACEBO
        )

        /** SVT-AV1 presets. */
        val svtAv1Presets = listOf(
            PRESET_0, PRESET_1, PRESET_2, PRESET_3, PRESET_4,
            PRESET_5, PRESET_6, PRESET_7, PRESET_8, PRESET_9,
            PRESET_10, PRESET_11, PRESET_12, PRESET_13
        )
    }
}
