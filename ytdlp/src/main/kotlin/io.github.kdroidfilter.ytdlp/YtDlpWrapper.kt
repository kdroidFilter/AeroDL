package com.example.ytdlpgui.ytdlp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal JVM wrapper around the yt-dlp CLI.
 *
 * This wrapper assumes yt-dlp binary is available on PATH.
 * It provides:
 * - availability check
 * - synchronous download execution with progress parsing via callbacks
 * - ability to cancel an in-flight download
 */
object YtDlp {
    data class Options(
        val format: String? = null, // e.g. "bestvideo+bestaudio/best"
        val outputTemplate: String? = null, // e.g. "%(title)s.%(ext)s"
        val extraArgs: List<String> = emptyList() // pass-through arguments
    )

    sealed class Event {
        data class Progress(
            val percent: Double?, // null if not parsed
            val rawLine: String
        ) : Event()
        data class Log(val line: String) : Event()
        data class Completed(val exitCode: Int) : Event()
        data class Error(val message: String, val cause: Throwable? = null) : Event()
        data object Started : Event()
        data object Cancelled : Event()
    }

    /** Simple handle that allows cancelling the running yt-dlp process. */
    class Handle internal constructor(private val process: Process, private val cancelledFlag: AtomicBoolean) {
        fun cancel() {
            cancelledFlag.set(true)
            process.destroy()
        }
        val isCancelled: Boolean get() = cancelledFlag.get()
    }

    /** Returns true if yt-dlp is available and returns a version string, or null otherwise. */
    fun version(): String? = try {
        val proc = ProcessBuilder(listOf("yt-dlp", "--version"))
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val code = proc.waitFor()
        if (code == 0 && out.isNotBlank()) out else null
    } catch (_: Exception) {
        null
    }

    fun isAvailable(): Boolean = version() != null

    /**
     * Runs yt-dlp for the given url.
     * This call is blocking until completion or cancellation. Use from a background thread if needed.
     *
     * @param url The media URL to download.
     * @param options CLI options mapping.
     * @param onEvent Callback for log/progress/completion events.
     * @return a Handle to allow cancellation.
     */
    fun download(
        url: String,
        options: Options = Options(),
        onEvent: (Event) -> Unit = {}
    ): Handle {
        val cmd = buildCommand(url, options)
        val pb = ProcessBuilder(cmd)
            .redirectErrorStream(true)
        val process = pb.start()
        val cancelled = AtomicBoolean(false)
        onEvent(Event.Started)

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!
                // Parse a very basic progress pattern from yt-dlp --newline output
                val progress = parseProgress(l)
                if (progress != null) {
                    onEvent(progress)
                } else {
                    onEvent(Event.Log(l))
                }
            }
        } catch (t: Throwable) {
            if (!cancelled.get()) {
                onEvent(Event.Error("I/O error while reading yt-dlp output", t))
            }
        } finally {
            reader.close()
        }

        val exit = try { process.waitFor() } catch (e: InterruptedException) { -1 }
        if (cancelled.get()) {
            onEvent(Event.Cancelled)
        } else {
            onEvent(Event.Completed(exit))
        }
        return Handle(process, cancelled)
    }

    private fun buildCommand(url: String, options: Options): List<String> {
        val cmd = mutableListOf("yt-dlp", "--newline")
        if (!options.format.isNullOrBlank()) {
            cmd.addAll(listOf("-f", options.format))
        }
        if (!options.outputTemplate.isNullOrBlank()) {
            cmd.addAll(listOf("-o", options.outputTemplate))
        }
        if (options.extraArgs.isNotEmpty()) {
            cmd.addAll(options.extraArgs)
        }
        cmd.add(url)
        return cmd
    }

    // Very permissive parser: extracts leading percent number like 3.1% from typical lines
    private fun parseProgress(line: String): Event.Progress? {
        // Common patterns from yt-dlp --newline:
        // [download]   3.1% of 10.00MiB at 1.23MiB/s ETA 00:10
        // [download] 100% of 10.00MiB in 00:08
        val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
        val match = percentRegex.find(line)
        val pct = match?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        return if (pct != null) Event.Progress(pct, line) else null
    }
}