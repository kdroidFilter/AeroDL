package io.github.kdroidfilter.ytdlp.util

internal var allowYtDlpLogging: Boolean = true
internal var loggingLevel: LoggingLevel = LoggingLevel.VERBOSE

internal class LoggingLevel(val priority: Int) {
    companion object {
        val VERBOSE = LoggingLevel(0)
        val DEBUG = LoggingLevel(1)
        val INFO = LoggingLevel(2)
        val WARN = LoggingLevel(3)
        val ERROR = LoggingLevel(4)
    }
}

private const val COLOR_RED = "\u001b[31m"
private const val COLOR_AQUA = "\u001b[36m"
private const val COLOR_LIGHT_GRAY = "\u001b[37m"
private const val COLOR_ORANGE = "\u001b[38;2;255;165;0m"
private const val COLOR_RESET = "\u001b[0m"

internal fun debugln(message: () -> String) {
    if (allowYtDlpLogging && loggingLevel.priority <= LoggingLevel.DEBUG.priority) {
        println(message())
    }
}

internal fun verboseln(message: () -> String) {
    if (allowYtDlpLogging && loggingLevel.priority <= LoggingLevel.VERBOSE.priority) {
        println(message(), COLOR_LIGHT_GRAY)
    }
}


internal fun infoln(message: () -> String) {
    if (allowYtDlpLogging && loggingLevel.priority <= LoggingLevel.INFO.priority) {
        println(message(), COLOR_AQUA)
    }
}

internal fun warnln(message: () -> String) {
    if (allowYtDlpLogging && loggingLevel.priority <= LoggingLevel.WARN.priority) {
        println(message(), COLOR_ORANGE)
    }
}

internal fun errorln(message: () -> String) {
    if (allowYtDlpLogging && loggingLevel.priority <= LoggingLevel.ERROR.priority) {
        println(message(), COLOR_RED)
    }
}

private fun println(message: String, color: String) {
    println(color + message + COLOR_RESET)
}