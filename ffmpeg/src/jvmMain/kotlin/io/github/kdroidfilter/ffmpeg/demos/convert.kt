package io.github.kdroidfilter.ffmpeg.demos

import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ffmpeg.core.ConversionEvent
import io.github.kdroidfilter.ffmpeg.core.ConversionOptions
import io.github.kdroidfilter.ffmpeg.core.InitEvent
import io.github.kdroidfilter.ffmpeg.model.EncoderPreset
import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.debugln
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Demo: Convert a video file to H.264 MP4.
 *
 * Usage: Set the `inputPath` variable to your input file and run.
 */
fun main() = runBlocking {
    LoggerConfig.enabled = true

    val inputPath = "/path/to/your/input.mkv"
    val outputPath = "/path/to/your/output.mp4"

    val inputFile = File(inputPath)
    val outputFile = File(outputPath)

    if (!inputFile.exists()) {
        errorln { "Input file not found: $inputPath" }
        return@runBlocking
    }

    val ffmpeg = FfmpegWrapper()

    // Initialize
    infoln { "Initializing FFmpeg..." }
    ffmpeg.initialize(downloadUrl = null, archiveName = null) { event ->
        when (event) {
            is InitEvent.Completed -> infoln { "FFmpeg ready!" }
            is InitEvent.Error -> {
                errorln { "Init error: ${event.message}" }
                return@initialize
            }
            else -> {}
        }
    }.join()

    // Get input duration for progress calculation
    val inputInfo = ffmpeg.analyze(inputFile).getOrNull()
    val totalDuration = inputInfo?.duration

    infoln { "Converting: ${inputFile.name}" }
    infoln { "Output: ${outputFile.name}" }
    infoln { "Duration: $totalDuration" }
    infoln { "=" .repeat(50) }

    // Convert with H.264
    val handle = ffmpeg.convert(
        inputFile = inputFile,
        outputFile = outputFile,
        options = ConversionOptions.h264(crf = 23, preset = EncoderPreset.MEDIUM)
    ) { event ->
        when (event) {
            is ConversionEvent.Started -> infoln { "Conversion started..." }

            is ConversionEvent.Progress -> {
                val percent = totalDuration?.let {
                    val p = event.timeProcessed.toMillis().toDouble() / it.toMillis() * 100
                    "%.1f%%".format(p.coerceIn(0.0, 100.0))
                } ?: event.timeProcessed.toString()

                val speed = event.speed?.let { "%.2fx".format(it) } ?: "N/A"
                val fps = event.fps?.let { "%.1f fps".format(it) } ?: ""

                infoln { "Progress: $percent | Speed: $speed $fps" }
            }

            is ConversionEvent.Log -> {
                debugln { event.line }
            }

            is ConversionEvent.Completed -> {
                infoln { "=" .repeat(50) }
                infoln { "Conversion completed!" }
                infoln { "Output: ${event.outputFile.absolutePath}" }
                infoln { "Size: ${event.outputFile.length() / 1024 / 1024} MB" }
            }

            is ConversionEvent.Error -> {
                errorln { "Error: ${event.message}" }
            }

            is ConversionEvent.Cancelled -> {
                infoln { "Conversion cancelled" }
            }
        }
    }

    // Wait for completion
    handle.job.join()

    ffmpeg.shutdown()
}
