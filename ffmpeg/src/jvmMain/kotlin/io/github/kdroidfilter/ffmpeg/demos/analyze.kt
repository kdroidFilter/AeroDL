package io.github.kdroidfilter.ffmpeg.demos

import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ffmpeg.core.InitEvent
import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.warnln
import io.github.kdroidfilter.logging.errorln
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Demo: Analyze a media file and print its information.
 *
 * Usage: Set the `inputPath` variable to a media file path and run.
 */
fun main() = runBlocking {
    LoggerConfig.enabled = true

    val inputPath = "/path/to/your/video.mp4"
    val inputFile = File(inputPath)

    if (!inputFile.exists()) {
        errorln { "File not found: $inputPath" }
        return@runBlocking
    }

    val ffmpeg = FfmpegWrapper()

    // Initialize FFmpeg
    infoln { "Initializing FFmpeg..." }
    ffmpeg.initialize(downloadUrl = null, archiveName = null) { event ->
        when (event) {
            is InitEvent.CheckingFfmpeg -> infoln { "Checking FFmpeg..." }
            is InitEvent.DownloadingFfmpeg -> infoln { "Downloading FFmpeg..." }
            is InitEvent.FfmpegProgress -> {
                val percent = event.percent?.let { "%.1f%%".format(it) } ?: "..."
                infoln { "Download progress: $percent" }
            }
            is InitEvent.Completed -> infoln { "FFmpeg ready!" }
            is InitEvent.Error -> errorln { "Error: ${event.message}" }
        }
    }.join()

    // Analyze the file
    infoln { "Analyzing: ${inputFile.name}" }
    infoln { "=" .repeat(50) }

    val result = ffmpeg.analyze(inputFile)

    result.fold(
        onSuccess = { info ->
            infoln { "Format: ${info.format.formatLongName ?: info.format.formatName}" }
            infoln { "Duration: ${info.duration}" }
            infoln { "Bitrate: ${info.format.bitRate?.let { "${it / 1000} kbps" } ?: "N/A"}" }
            infoln { "File size: ${info.fileSize / 1024 / 1024} MB" }
            infoln { "" }

            if (info.videoStreams.isNotEmpty()) {
                infoln { "Video Streams:" }
                info.videoStreams.forEachIndexed { index, video ->
                    infoln { "  [$index] ${video.codec} ${video.resolution}" }
                    infoln { "       Frame rate: ${video.frameRate?.let { "%.2f fps".format(it) } ?: "N/A"}" }
                    infoln { "       Bit depth: ${video.bitDepth ?: 8}-bit" }
                    infoln { "       Pixel format: ${video.pixelFormat}" }
                    infoln { "       HDR: ${video.isHdr}" }
                    video.language?.let { infoln { "       Language: $it" } }
                }
                infoln { "" }
            }

            if (info.audioStreams.isNotEmpty()) {
                infoln { "Audio Streams:" }
                info.audioStreams.forEachIndexed { index, audio ->
                    infoln { "  [$index] ${audio.codec} ${audio.channels}ch @ ${audio.sampleRate} Hz" }
                    audio.bitRate?.let { infoln { "       Bitrate: ${it / 1000} kbps" } }
                    audio.language?.let { infoln { "       Language: $it" } }
                }
                infoln { "" }
            }

            if (info.subtitleStreams.isNotEmpty()) {
                infoln { "Subtitle Streams:" }
                info.subtitleStreams.forEachIndexed { index, sub ->
                    infoln { "  [$index] ${sub.codec} - ${sub.language ?: "Unknown"}" }
                    sub.title?.let { infoln { "       Title: $it" } }
                }
            }
        },
        onFailure = { error ->
            errorln { "Analysis failed: ${error.message}" }
        }
    )

    ffmpeg.shutdown()
}
