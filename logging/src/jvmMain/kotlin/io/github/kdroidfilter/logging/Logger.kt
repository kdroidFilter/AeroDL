package io.github.kdroidfilter.logging

import io.sentry.Sentry
import io.sentry.SentryLevel
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Unified lightweight logging for JVM modules.
 * Provides level filtering, optional timestamps, and ANSI colors.
 */
object LoggerConfig {
    @Volatile var enabled: Boolean = System.getProperty("debugLogs", "false").toBoolean()
    @Volatile var level: LoggingLevel = LoggingLevel.VERBOSE
    @Volatile var showTimestamp: Boolean = true
    @Volatile var useColors: Boolean = true
    @Volatile var sentryEnabled: Boolean = true
    @Volatile var sentryLevel: LoggingLevel = LoggingLevel.ERROR
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


private fun shouldLogToConsole(minLevel: LoggingLevel): Boolean {
    return LoggerConfig.enabled && LoggerConfig.level.priority <= minLevel.priority
}

private fun shouldLogToSentry(minLevel: LoggingLevel): Boolean {
    return LoggerConfig.sentryEnabled &&
        Sentry.isEnabled() &&
        minLevel.priority >= LoggerConfig.sentryLevel.priority
}

private fun toSentryLevel(level: LoggingLevel): SentryLevel = when (level.priority) {
    LoggingLevel.VERBOSE.priority -> SentryLevel.DEBUG
    LoggingLevel.DEBUG.priority -> SentryLevel.DEBUG
    LoggingLevel.INFO.priority -> SentryLevel.INFO
    LoggingLevel.WARN.priority -> SentryLevel.WARNING
    else -> SentryLevel.ERROR
}

private fun logAt(
    minLevel: LoggingLevel,
    color: String,
    tag: String,
    throwable: Throwable? = null,
    message: () -> String,
) {
    val sendToConsole = shouldLogToConsole(minLevel)
    val sendToSentry = shouldLogToSentry(minLevel)
    if (!sendToConsole && !sendToSentry) return

    val ts = if (LoggerConfig.showTimestamp) "[${nowString()}] " else ""
    val renderedMessage = message()
    val content = "[$tag] $ts$renderedMessage"

    if (sendToConsole) {
        if (LoggerConfig.useColors) println(color + content + COLOR_RESET) else kotlin.io.println(content)
        throwable?.let { it.printStackTrace() }
    }

    if (sendToSentry) {
        runCatching {
            val sentryLevel = toSentryLevel(minLevel)
            Sentry.withScope { scope ->
                scope.level = sentryLevel
                scope.setTag("logger.tag", tag)
                if (throwable != null) {
                    if (renderedMessage.isNotBlank()) {
                        scope.setExtra("logger.message", renderedMessage)
                    }
                    Sentry.captureException(throwable)
                } else {
                    Sentry.captureMessage(renderedMessage, sentryLevel)
                }
            }
        }
    }
}

fun verboseln(message: () -> String) =
    logAt(LoggingLevel.VERBOSE, COLOR_LIGHT_GRAY, "VERBOSE", message = message)

fun debugln(message: () -> String) =
    logAt(LoggingLevel.DEBUG, COLOR_LIGHT_GRAY, "DEBUG", message = message)

fun infoln(message: () -> String) =
    logAt(LoggingLevel.INFO, COLOR_AQUA, "INFO", message = message)

fun warnln(message: () -> String) =
    logAt(LoggingLevel.WARN, COLOR_ORANGE, "WARN", message = message)

fun errorln(message: () -> String) =
    logAt(LoggingLevel.ERROR, COLOR_RED, "ERROR", message = message)

fun warnln(throwable: Throwable, message: () -> String = { throwable.message ?: "Warning" }) =
    logAt(LoggingLevel.WARN, COLOR_ORANGE, "WARN", throwable = throwable, message = message)

fun errorln(throwable: Throwable, message: () -> String = { throwable.message ?: "Error" }) =
    logAt(LoggingLevel.ERROR, COLOR_RED, "ERROR", throwable = throwable, message = message)
