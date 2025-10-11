package io.github.kdroidfilter.logging

import java.text.SimpleDateFormat
import java.util.Date

/**
 * Unified lightweight logging for JVM modules.
 * Provides level filtering, optional timestamps, and ANSI colors.
 */
object LoggerConfig {
    @Volatile var enabled: Boolean = true
    @Volatile var level: LoggingLevel = LoggingLevel.VERBOSE
    @Volatile var showTimestamp: Boolean = true
    @Volatile var useColors: Boolean = true
}

class LoggingLevel(val priority: Int) {
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

private fun nowString(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())

private inline fun logAt(minLevel: LoggingLevel, color: String, tag: String, crossinline message: () -> String) {
    if (!LoggerConfig.enabled || LoggerConfig.level.priority > minLevel.priority) return
    val ts = if (LoggerConfig.showTimestamp) "[${nowString()}] " else ""
    val content = "[$tag] $ts${message()}"
    if (LoggerConfig.useColors) println(color + content + COLOR_RESET) else kotlin.io.println(content)
}

inline fun verboseln(noinline message: () -> String) =
    logAt(LoggingLevel.VERBOSE, COLOR_LIGHT_GRAY, "VERBOSE", message)

inline fun debugln(noinline message: () -> String) =
    logAt(LoggingLevel.DEBUG, COLOR_LIGHT_GRAY, "DEBUG", message)

inline fun infoln(noinline message: () -> String) =
    logAt(LoggingLevel.INFO, COLOR_AQUA, "INFO", message)

inline fun warnln(noinline message: () -> String) =
    logAt(LoggingLevel.WARN, COLOR_ORANGE, "WARN", message)

inline fun errorln(noinline message: () -> String) =
    logAt(LoggingLevel.ERROR, COLOR_RED, "ERROR", message)

