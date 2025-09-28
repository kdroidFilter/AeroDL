package io.github.kdroidfilter.ytdlp.core

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents the various events that can be emitted during a download process.
 */
sealed class Event {
    /** Indicates download progress. */
    data class Progress(val percent: Double?, val rawLine: String) : Event()

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
 * @property process The underlying Java [Process].
 * @property cancelled An atomic boolean to track the cancellation state.
 */
data class Handle(
    val process: Process,
    val cancelled: AtomicBoolean
) {
    /**
     * Cancels the running download process.
     */
    fun cancel() {
        if (!cancelled.getAndSet(true)) {
            process.destroy()
        }
    }
}