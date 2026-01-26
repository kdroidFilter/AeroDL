package io.github.kdroidfilter.ffmpeg.util

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Hardware acceleration detection and management for FFmpeg.
 * Automatically detects available hardware encoders on the system.
 */
object HardwareAcceleration {

    /**
     * Hardware encoder types ordered by preference.
     */
    enum class HwEncoder(
        val ffmpegName: String,
        val displayName: String,
        val platform: Platform,
        val hwaccel: String? = null
    ) {
        // Windows encoders
        H264_NVENC("h264_nvenc", "H.264 NVIDIA NVENC", Platform.WINDOWS),
        H264_QSV("h264_qsv", "H.264 Intel QuickSync", Platform.WINDOWS),
        H264_AMF("h264_amf", "H.264 AMD AMF", Platform.WINDOWS),

        // Linux encoders
        H264_NVENC_LINUX("h264_nvenc", "H.264 NVIDIA NVENC", Platform.LINUX),
        H264_VAAPI("h264_vaapi", "H.264 VA-API", Platform.LINUX, "vaapi"),

        // macOS encoder
        H264_VIDEOTOOLBOX("h264_videotoolbox", "H.264 VideoToolbox", Platform.MACOS),

        // HEVC variants
        HEVC_NVENC("hevc_nvenc", "HEVC NVIDIA NVENC", Platform.WINDOWS),
        HEVC_QSV("hevc_qsv", "HEVC Intel QuickSync", Platform.WINDOWS),
        HEVC_AMF("hevc_amf", "HEVC AMD AMF", Platform.WINDOWS),
        HEVC_NVENC_LINUX("hevc_nvenc", "HEVC NVIDIA NVENC", Platform.LINUX),
        HEVC_VAAPI("hevc_vaapi", "HEVC VA-API", Platform.LINUX, "vaapi"),
        HEVC_VIDEOTOOLBOX("hevc_videotoolbox", "HEVC VideoToolbox", Platform.MACOS);

        enum class Platform { WINDOWS, LINUX, MACOS, ANY }
    }

    private var cachedH264Encoder: String? = null
    private var cachedHevcEncoder: String? = null
    private var detectionDone = false

    /**
     * Current platform.
     */
    val currentPlatform: HwEncoder.Platform by lazy {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> HwEncoder.Platform.WINDOWS
            os.contains("mac") || os.contains("darwin") -> HwEncoder.Platform.MACOS
            else -> HwEncoder.Platform.LINUX
        }
    }

    /**
     * Detects available hardware encoders by running FFmpeg.
     * Results are cached after first detection.
     *
     * @param ffmpegPath Path to the FFmpeg binary
     * @return List of available hardware encoder names
     */
    fun detectAvailableEncoders(ffmpegPath: String): List<String> {
        return try {
            val process = ProcessBuilder(ffmpegPath, "-hide_banner", "-encoders")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(10, TimeUnit.SECONDS)

            // Parse encoder list - lines starting with V for video encoders
            output.lines()
                .filter { it.trim().startsWith("V") }
                .mapNotNull { line ->
                    // Format: " V..... encoder_name  Description"
                    line.trim().split(Regex("\\s+")).getOrNull(1)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets the best available H.264 hardware encoder for the current platform.
     * Falls back to libx264 if no hardware encoder is available.
     *
     * @param ffmpegPath Path to FFmpeg binary
     * @return The encoder name to use (e.g., "h264_nvenc" or "libx264")
     */
    fun getBestH264Encoder(ffmpegPath: String): String {
        if (detectionDone && cachedH264Encoder != null) {
            return cachedH264Encoder!!
        }

        val availableEncoders = detectAvailableEncoders(ffmpegPath)

        // Get platform-specific encoders in preference order
        val preferredEncoders = when (currentPlatform) {
            HwEncoder.Platform.WINDOWS -> listOf("h264_nvenc", "h264_qsv", "h264_amf")
            HwEncoder.Platform.MACOS -> listOf("h264_videotoolbox")
            HwEncoder.Platform.LINUX -> listOf("h264_nvenc", "h264_vaapi")
            else -> emptyList()
        }

        // Find first available hardware encoder
        val hwEncoder = preferredEncoders.firstOrNull { it in availableEncoders }

        cachedH264Encoder = hwEncoder ?: "libx264"
        detectionDone = true

        return cachedH264Encoder!!
    }

    /**
     * Gets the best available HEVC hardware encoder for the current platform.
     * Falls back to libx265 if no hardware encoder is available.
     *
     * @param ffmpegPath Path to FFmpeg binary
     * @return The encoder name to use (e.g., "hevc_nvenc" or "libx265")
     */
    fun getBestHevcEncoder(ffmpegPath: String): String {
        if (detectionDone && cachedHevcEncoder != null) {
            return cachedHevcEncoder!!
        }

        val availableEncoders = detectAvailableEncoders(ffmpegPath)

        val preferredEncoders = when (currentPlatform) {
            HwEncoder.Platform.WINDOWS -> listOf("hevc_nvenc", "hevc_qsv", "hevc_amf")
            HwEncoder.Platform.MACOS -> listOf("hevc_videotoolbox")
            HwEncoder.Platform.LINUX -> listOf("hevc_nvenc", "hevc_vaapi")
            else -> emptyList()
        }

        val hwEncoder = preferredEncoders.firstOrNull { it in availableEncoders }

        cachedHevcEncoder = hwEncoder ?: "libx265"

        return cachedHevcEncoder!!
    }

    /**
     * Checks if a specific encoder is a hardware encoder.
     */
    fun isHardwareEncoder(encoderName: String): Boolean {
        return encoderName in listOf(
            "h264_nvenc", "h264_qsv", "h264_amf", "h264_vaapi", "h264_videotoolbox",
            "hevc_nvenc", "hevc_qsv", "hevc_amf", "hevc_vaapi", "hevc_videotoolbox"
        )
    }

    /**
     * Gets the hwaccel option for VA-API if needed.
     */
    fun getHwAccelOption(encoderName: String): String? {
        return when (encoderName) {
            "h264_vaapi", "hevc_vaapi" -> "vaapi"
            else -> null
        }
    }

    /**
     * Gets additional arguments needed for specific hardware encoders.
     */
    fun getEncoderExtraArgs(encoderName: String): List<String> {
        return when (encoderName) {
            "h264_vaapi", "hevc_vaapi" -> {
                // VA-API requires device specification on Linux
                val renderDevice = findVaapiDevice()
                if (renderDevice != null) {
                    listOf("-vaapi_device", renderDevice)
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    /**
     * Finds the VA-API render device on Linux.
     */
    private fun findVaapiDevice(): String? {
        val renderD128 = File("/dev/dri/renderD128")
        val renderD129 = File("/dev/dri/renderD129")

        return when {
            renderD128.exists() -> "/dev/dri/renderD128"
            renderD129.exists() -> "/dev/dri/renderD129"
            else -> null
        }
    }

    /**
     * Clears the cached encoder detection results.
     * Call this if the system configuration might have changed.
     */
    fun clearCache() {
        cachedH264Encoder = null
        cachedHevcEncoder = null
        detectionDone = false
    }
}
