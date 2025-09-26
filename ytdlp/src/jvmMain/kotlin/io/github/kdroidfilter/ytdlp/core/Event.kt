package io.github.kdroidfilter.ytdlp.core

import java.util.concurrent.atomic.AtomicBoolean

sealed class Event {
    data class Progress(val percent: Double?, val rawLine: String) : Event()
    data class Log(val line: String) : Event()
    data class Error(val message: String, val cause: Throwable? = null) : Event()
    data class Completed(val exitCode: Int, val success: Boolean) : Event()
    data object Cancelled : Event()
    data object Started : Event()
    data class NetworkProblem(val detail: String) : Event()
}

data class Handle(
    val process: Process,
    val cancelled: AtomicBoolean
) {
    fun cancel() {
        cancelled.set(true)
        process.destroy()
    }
}
