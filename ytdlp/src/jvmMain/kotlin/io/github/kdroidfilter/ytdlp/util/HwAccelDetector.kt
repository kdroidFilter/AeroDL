package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.logging.infoln
import java.util.concurrent.TimeUnit

/**
 * Detects available FFmpeg hardware encoders for use with yt-dlp post-processing.
 * Supports NVENC (NVIDIA), QSV (Intel), AMF (AMD), VideoToolbox (macOS), and VAAPI (Linux).
 */
object HwAccelDetector {

    enum class Platform { WINDOWS, LINUX, MACOS }

    @Volatile
    private var cachedH264Encoder: String? = null

    @Volatile
    private var detectionAttempted: Boolean = false

    val currentPlatform: Platform by lazy {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> Platform.WINDOWS
            os.contains("mac") || os.contains("darwin") -> Platform.MACOS
            else -> Platform.LINUX
        }
    }

    /**
     * Gets the best available H.264 hardware encoder for yt-dlp post-processing.
     * Returns null if no hardware encoder is available (will use software encoding).
     *
     * @param ffmpegPath Path to the FFmpeg executable
     * @return The encoder name (e.g., "h264_nvenc") or null if none available
     */
    fun getBestH264Encoder(ffmpegPath: String): String? {
        if (detectionAttempted) return cachedH264Encoder

        synchronized(this) {
            if (detectionAttempted) return cachedH264Encoder

            val available = detectAvailableEncoders(ffmpegPath)

            val preferred = when (currentPlatform) {
                Platform.WINDOWS -> listOf("h264_nvenc", "h264_qsv", "h264_amf")
                Platform.MACOS -> listOf("h264_videotoolbox")
                Platform.LINUX -> listOf("h264_nvenc", "h264_vaapi")
            }

            cachedH264Encoder = preferred.firstOrNull { it in available }
            detectionAttempted = true

            if (cachedH264Encoder != null) {
                infoln { "[HwAccelDetector] Detected hardware encoder: $cachedH264Encoder" }
            } else {
                infoln { "[HwAccelDetector] No hardware encoder available, will use software encoding" }
            }

            return cachedH264Encoder
        }
    }

    private fun detectAvailableEncoders(ffmpegPath: String): List<String> {
        return try {
            val process = ProcessBuilder(ffmpegPath, "-hide_banner", "-encoders")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exited = process.waitFor(10, TimeUnit.SECONDS)

            if (!exited) {
                process.destroyForcibly()
                return emptyList()
            }

            output.lines()
                .filter { it.trim().startsWith("V") }
                .mapNotNull { line -> line.trim().split(Regex("\\s+")).getOrNull(1) }
        } catch (e: Exception) {
            infoln { "[HwAccelDetector] Failed to detect encoders: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Generates postprocessor args for hardware-accelerated H.264 encoding.
     * Returns null if no hardware encoder is available.
     *
     * @param ffmpegPath Path to the FFmpeg executable
     * @return The postprocessor args string or null if no HW encoder available
     */
    fun getHwPostprocessorArgs(ffmpegPath: String): String? {
        val encoder = getBestH264Encoder(ffmpegPath) ?: return null

        return when (encoder) {
            "h264_nvenc" -> "ffmpeg:-c:v h264_nvenc -preset p4 -cq 23"
            "h264_qsv" -> "ffmpeg:-c:v h264_qsv -preset medium -global_quality 23"
            "h264_amf" -> "ffmpeg:-c:v h264_amf -quality balanced -qp_i 23 -qp_p 23"
            "h264_videotoolbox" -> "ffmpeg:-c:v h264_videotoolbox -q:v 65"
            "h264_vaapi" -> "ffmpeg:-c:v h264_vaapi -qp 23"
            else -> null
        }
    }

    /**
     * Clears the cached encoder detection result.
     * Call this if the FFmpeg installation changes.
     */
    fun clearCache() {
        synchronized(this) {
            cachedH264Encoder = null
            detectionAttempted = false
        }
    }
}
