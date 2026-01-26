package io.github.kdroidfilter.ffmpeg.util

import java.time.Duration

/**
 * Parses FFmpeg progress output from stderr.
 */
object ProgressParser {

    // time=00:01:23.45 format
    private val timeRegex = Regex("""time=(\d+):(\d+):(\d+)\.(\d+)""")

    // speed=1.5x format
    private val speedRegex = Regex("""speed=\s*([\d.]+)x""")

    // fps=30 format
    private val fpsRegex = Regex("""fps=\s*([\d.]+)""")

    // bitrate=1234.5kbits/s format
    private val bitrateRegex = Regex("""bitrate=\s*([\d.]+)(k?bits/s)""")

    // frame=1234 format
    private val frameRegex = Regex("""frame=\s*(\d+)""")

    // size=1234kB format
    private val sizeRegex = Regex("""size=\s*([\d.]+)(k?B)""")

    /**
     * Data class holding parsed progress information.
     */
    data class ProgressData(
        val time: Duration,
        val speed: Double?,
        val fps: Double?,
        val bitrate: Long?,
        val frame: Long?,
        val size: Long?
    )

    /**
     * Parse a line of FFmpeg output for progress information.
     * Returns null if the line doesn't contain progress data.
     */
    fun parse(line: String): ProgressData? {
        val timeMatch = timeRegex.find(line) ?: return null

        val hours = timeMatch.groupValues[1].toLongOrNull() ?: 0
        val minutes = timeMatch.groupValues[2].toLongOrNull() ?: 0
        val seconds = timeMatch.groupValues[3].toLongOrNull() ?: 0
        val centiseconds = timeMatch.groupValues[4].padEnd(2, '0').take(2).toLongOrNull() ?: 0

        val time = Duration.ofHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds)
            .plusMillis(centiseconds * 10)

        val speed = speedRegex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
        val fps = fpsRegex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
        val bitrate = parseBitrate(line)
        val frame = frameRegex.find(line)?.groupValues?.get(1)?.toLongOrNull()
        val size = parseSize(line)

        return ProgressData(
            time = time,
            speed = speed,
            fps = fps,
            bitrate = bitrate,
            frame = frame,
            size = size
        )
    }

    private fun parseBitrate(line: String): Long? {
        val match = bitrateRegex.find(line) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2]

        // Convert to bits per second
        return when {
            unit.startsWith("k") -> (value * 1000).toLong()
            else -> value.toLong()
        }
    }

    private fun parseSize(line: String): Long? {
        val match = sizeRegex.find(line) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2]

        // Convert to bytes
        return when {
            unit.startsWith("k") -> (value * 1024).toLong()
            else -> value.toLong()
        }
    }

    /**
     * Calculate progress percentage based on processed time and total duration.
     */
    fun calculatePercent(processedTime: Duration, totalDuration: Duration?): Double? {
        if (totalDuration == null || totalDuration.isZero || totalDuration.isNegative) {
            return null
        }
        val percent = processedTime.toMillis().toDouble() / totalDuration.toMillis().toDouble() * 100
        return percent.coerceIn(0.0, 100.0)
    }
}

/**
 * Diagnoses common FFmpeg errors from output.
 */
object ErrorDiagnostics {

    private val errorPatterns = listOf(
        "No such file or directory" to "Input file not found",
        "Invalid data found" to "Invalid or corrupted input file",
        "Permission denied" to "Permission denied accessing file",
        "Unknown encoder" to "Encoder not available (may need to be compiled into FFmpeg)",
        "Encoder .* not found" to "Encoder not available",
        "does not contain any stream" to "Input file has no media streams",
        "Output file is empty" to "Encoding failed - no output produced",
        "Option .* not found" to "Invalid FFmpeg option",
        "Unrecognized option" to "Unrecognized FFmpeg option",
        "Could not find codec" to "Codec not found",
        "Decoder .* not found" to "Decoder not available",
        "Error .* codec" to "Codec error",
        "Error opening" to "Could not open file",
        "Unable to find a suitable output format" to "Unknown output format",
        "Cannot allocate memory" to "Out of memory",
        "Invalid argument" to "Invalid argument provided"
    )

    /**
     * Analyze FFmpeg output lines to find a human-readable error message.
     */
    fun diagnose(lines: List<String>): String? {
        for (line in lines.reversed()) {
            for ((pattern, message) in errorPatterns) {
                if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                    return message
                }
            }
        }
        return null
    }
}
