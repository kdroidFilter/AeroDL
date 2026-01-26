package io.github.kdroidfilter.ffmpeg.model

/**
 * Video bitrate presets.
 */
enum class VideoBitrate(val bitsPerSecond: Long, val displayName: String) {
    M1(1_000_000, "1 Mbps"),
    M2(2_000_000, "2 Mbps"),
    M3(3_000_000, "3 Mbps"),
    M4(4_000_000, "4 Mbps"),
    M5(5_000_000, "5 Mbps"),
    M6(6_000_000, "6 Mbps"),
    M8(8_000_000, "8 Mbps"),
    M10(10_000_000, "10 Mbps"),
    M12(12_000_000, "12 Mbps"),
    M15(15_000_000, "15 Mbps"),
    M20(20_000_000, "20 Mbps"),
    M25(25_000_000, "25 Mbps"),
    M30(30_000_000, "30 Mbps"),
    M40(40_000_000, "40 Mbps"),
    M50(50_000_000, "50 Mbps");

    val ffmpegValue: String get() = "${bitsPerSecond / 1_000_000}M"

    companion object {
        fun fromBitsPerSecond(bps: Long): VideoBitrate? =
            entries.minByOrNull { kotlin.math.abs(it.bitsPerSecond - bps) }
    }
}

/**
 * Audio bitrate presets.
 */
enum class AudioBitrate(val bitsPerSecond: Long, val displayName: String) {
    K32(32_000, "32 kbps"),
    K48(48_000, "48 kbps"),
    K64(64_000, "64 kbps"),
    K80(80_000, "80 kbps"),
    K96(96_000, "96 kbps"),
    K112(112_000, "112 kbps"),
    K128(128_000, "128 kbps"),
    K160(160_000, "160 kbps"),
    K192(192_000, "192 kbps"),
    K224(224_000, "224 kbps"),
    K256(256_000, "256 kbps"),
    K288(288_000, "288 kbps"),
    K320(320_000, "320 kbps"),
    K384(384_000, "384 kbps"),
    K448(448_000, "448 kbps"),
    K512(512_000, "512 kbps");

    val ffmpegValue: String get() = "${bitsPerSecond / 1_000}k"

    companion object {
        fun fromBitsPerSecond(bps: Long): AudioBitrate? =
            entries.minByOrNull { kotlin.math.abs(it.bitsPerSecond - bps) }
    }
}
