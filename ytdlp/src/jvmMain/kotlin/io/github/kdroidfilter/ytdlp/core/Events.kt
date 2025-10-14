package io.github.kdroidfilter.ytdlp.core

import kotlinx.coroutines.Job

/**
 * Represents the various events that can be emitted during a download process.
 */
sealed class Event {
    /** Indicates download progress. */
    data class Progress(val percent: Double?, val speedBytesPerSec: Long?, val rawLine: String) : Event()

    /** A generic log message from the yt-dlp process. */
    data class Log(val line: String) : Event()

    /** An error occurred during the process. */
    data class Error(val message: String, val cause: Throwable? = null) : Event()

    /** The process has finished. */
    data class Completed(val exitCode: Int, val success: Boolean) : Event()

    /** The process was cancelled by the user. */
    data object Cancelled : Event()

    /** The process has started. */
    data object Started : Event()

    /** A network issue was detected before the process could start. */
    data class NetworkProblem(val detail: String) : Event()
}

/**
 * A handle to a running download process, allowing for cancellation.
 * @property job The underlying coroutine [Job] controlling the download lifecycle.
 */
data class Handle(
    val job: Job
) {
    /**
     * Cancels the running download process by cancelling its coroutine job.
     */
    fun cancel() {
        job.cancel()
    }
}
