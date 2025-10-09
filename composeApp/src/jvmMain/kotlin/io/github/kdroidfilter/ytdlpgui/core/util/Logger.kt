package io.github.kdroidfilter.ytdlpgui.core.util

import java.text.SimpleDateFormat
import java.util.Date

private const val COLOR_RED = "\u001b[31m"
private const val COLOR_AQUA = "\u001b[36m"
private const val COLOR_LIGHT_GRAY = "\u001b[37m"
private const val COLOR_ORANGE = "\u001b[38;2;255;165;0m"
private const val COLOR_RESET = "\u001b[0m"

var loggingLevel: LoggingLevel = LoggingLevel.VERBOSE

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    return sdf.format(Date())
}

fun debugln(message: () -> String) {
    if (loggingLevel.priority <= LoggingLevel.DEBUG.priority) {
        println("[DEBUG] [${getCurrentTime()}] ${message()}")
    }
}

fun infoln(message: () -> String) {
    if (loggingLevel.priority <= LoggingLevel.INFO.priority) {
        println("$COLOR_AQUA[INFO] [${getCurrentTime()}] ${message()}$COLOR_RESET")
    }
}

fun warnln(message: () -> String) {
    if (loggingLevel.priority <= LoggingLevel.WARN.priority) {
        println("$COLOR_ORANGE[WARN] [${getCurrentTime()}] ${message()}$COLOR_RESET")
    }
}

fun errorln(message: () -> String) {
    if (loggingLevel.priority <= LoggingLevel.ERROR.priority) {
        println("$COLOR_RED[ERROR] [${getCurrentTime()}] ${message()}$COLOR_RESET")
    }
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