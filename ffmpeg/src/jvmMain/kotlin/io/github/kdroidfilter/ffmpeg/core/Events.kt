package io.github.kdroidfilter.ffmpeg.core

import kotlinx.coroutines.Job
import java.io.File
import java.time.Duration

/**
 * Represents the various events that can be emitted during an FFmpeg conversion process.
 */
sealed class ConversionEvent {
    /** Indicates conversion progress. */
    data class Progress(
        val timeProcessed: Duration,
        val speed: Double?,
        val fps: Double?,
        val bitrate: Long?,
        val rawLine: String
    ) : ConversionEvent()

    /** A generic log message from the FFmpeg process. */
    data class Log(val line: String) : ConversionEvent()

    /** An error occurred during the process. */
    data class Error(val message: String, val cause: Throwable? = null) : ConversionEvent()

    /** The process has finished successfully. */
    data class Completed(val outputFile: File, val exitCode: Int) : ConversionEvent()

    /** The process was cancelled by the user. */
    data object Cancelled : ConversionEvent()

    /** The process has started. */
    data object Started : ConversionEvent()
}

/**
 * Events emitted during FFprobe media analysis.
 */
sealed class ProbeEvent {
    /** Analysis completed successfully. */
    data class Completed(val info: io.github.kdroidfilter.ffmpeg.model.MediaInfo) : ProbeEvent()

    /** An error occurred during analysis. */
    data class Error(val message: String, val cause: Throwable? = null) : ProbeEvent()
}

/**
 * Events emitted during FFmpeg initialization.
 */
sealed interface InitEvent {
    data object CheckingFfmpeg : InitEvent
    data object DownloadingFfmpeg : InitEvent
    data class FfmpegProgress(val bytesRead: Long, val totalBytes: Long?, val percent: Double?) : InitEvent
    data class Completed(val success: Boolean) : InitEvent
    data class Error(val message: String, val cause: Throwable? = null) : InitEvent
}

/**
 * A handle to a running FFmpeg process, allowing for cancellation.
 * @property job The underlying coroutine [Job] controlling the conversion lifecycle.
 */
data class ConversionHandle(
    val job: Job
) {
    /**
     * Cancels the running FFmpeg process by cancelling its coroutine job.
     */
    fun cancel() {
        job.cancel()
    }
}
