package io.github.kdroidfilter.ytdlp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JVM wrapper around yt-dlp CLI, with configurable binary paths.
 */
object YtDlp {
    /** Configuration for paths */
    var ytDlpPath: String = "yt-dlp"
    var ffmpegPath: String? = null
    var downloadDir: File? = null

    data class Options(
        val format: String? = null,            // e.g. "bestvideo+bestaudio/best"
        val outputTemplate: String? = null,    // e.g. "%(title)s.%(ext)s"
        val noCheckCertificate: Boolean = false,
        val extraArgs: List<String> = emptyList()
    )

    sealed class Event {
        data class Progress(val percent: Double?, val rawLine: String) : Event()
        data class Log(val line: String) : Event()
        data class Completed(val exitCode: Int) : Event()
        data class Error(val message: String, val cause: Throwable? = null) : Event()
        data object Started : Event()
        data object Cancelled : Event()
    }

    class Handle internal constructor(private val process: Process, private val cancelledFlag: AtomicBoolean) {
        fun cancel() {
            cancelledFlag.set(true)
            process.destroy()
        }
        val isCancelled: Boolean get() = cancelledFlag.get()
    }

    /** Returns yt-dlp version or null if unavailable */
    fun version(): String? = try {
        val proc = ProcessBuilder(listOf(ytDlpPath, "--version"))
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
     * Run yt-dlp with the given url and options.
     * Blocking until finished/cancelled. Run from a background thread.
     */
    fun download(
        url: String,
        options: Options = Options(),
        onEvent: (Event) -> Unit = {}
    ): Handle {
        val cmd = buildCommand(url, options)
        val pb = ProcessBuilder(cmd)
            .redirectErrorStream(true)

        // configure working directory if provided
        downloadDir?.let { pb.directory(it) }

        val process = pb.start()
        val cancelled = AtomicBoolean(false)
        onEvent(Event.Started)

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!
                val progress = parseProgress(l)
                if (progress != null) onEvent(progress) else onEvent(Event.Log(l))
            }
        } catch (t: Throwable) {
            if (!cancelled.get()) onEvent(Event.Error("I/O error while reading yt-dlp output", t))
        } finally {
            reader.close()
        }

        val exit = try { process.waitFor() } catch (e: InterruptedException) { -1 }
        if (cancelled.get()) onEvent(Event.Cancelled) else onEvent(Event.Completed(exit))
        return Handle(process, cancelled)
    }

    private fun buildCommand(url: String, options: Options): List<String> {
        val cmd = mutableListOf(ytDlpPath, "--newline")

        // ffmpeg override
        if (!ffmpegPath.isNullOrBlank()) {
            cmd.addAll(listOf("--ffmpeg-location", ffmpegPath!!))
        }

        // no-check-certificate
        if (options.noCheckCertificate) {
            cmd.add("--no-check-certificate")
        }

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

    private fun parseProgress(line: String): Event.Progress? {
        val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
        val match = percentRegex.find(line)
        val pct = match?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        return if (pct != null) Event.Progress(pct, line) else null
    }
}