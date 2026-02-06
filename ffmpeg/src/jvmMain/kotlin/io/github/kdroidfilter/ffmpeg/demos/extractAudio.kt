package io.github.kdroidfilter.ffmpeg.demos

import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ffmpeg.core.ConversionEvent
import io.github.kdroidfilter.ffmpeg.core.InitEvent
import io.github.kdroidfilter.ffmpeg.model.AudioBitrate
import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.errorln
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Demo: Extract audio from a video file to MP3.
 *
 * Usage: Set the `inputPath` variable to your input file and run.
 */
fun main() = runBlocking {
    LoggerConfig.enabled = true

    val inputPath = "/path/to/your/video.mp4"
    val outputPath = "/path/to/your/audio.mp3"

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

    // Get input info
    val inputInfo = ffmpeg.analyze(inputFile).getOrNull()
    val totalDuration = inputInfo?.duration

    infoln { "Extracting audio from: ${inputFile.name}" }
    infoln { "Output: ${outputFile.name}" }
    inputInfo?.primaryAudio?.let { audio ->
        infoln { "Source audio: ${audio.codec} ${audio.channels}ch @ ${audio.sampleRate} Hz" }
    }
    infoln { "=" .repeat(50) }

    // Extract audio to MP3 at 320 kbps
    val handle = ffmpeg.extractAudioMp3(
        inputFile = inputFile,
        outputFile = outputFile,
        bitrate = AudioBitrate.K320
    ) { event ->
        when (event) {
            is ConversionEvent.Started -> infoln { "Extraction started..." }

            is ConversionEvent.Progress -> {
                val percent = totalDuration?.let {
                    val p = event.timeProcessed.toMillis().toDouble() / it.toMillis() * 100
                    "%.1f%%".format(p.coerceIn(0.0, 100.0))
                } ?: event.timeProcessed.toString()

                val speed = event.speed?.let { "%.2fx".format(it) } ?: "N/A"
                infoln { "Progress: $percent | Speed: $speed" }
            }

            is ConversionEvent.Completed -> {
                infoln { "=" .repeat(50) }
                infoln { "Audio extraction completed!" }
                infoln { "Output: ${event.outputFile.absolutePath}" }
                infoln { "Size: ${event.outputFile.length() / 1024} KB" }
            }

            is ConversionEvent.Error -> {
                errorln { "Error: ${event.message}" }
            }

            else -> {}
        }
    }

    handle.job.join()
    ffmpeg.shutdown()
}
