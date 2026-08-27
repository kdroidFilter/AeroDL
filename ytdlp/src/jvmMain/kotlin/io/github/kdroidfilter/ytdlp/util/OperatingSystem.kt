package io.github.kdroidfilter.ytdlp.util

enum class OperatingSystem {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN,
}

fun getOperatingSystem(): OperatingSystem {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        os.contains("win") -> OperatingSystem.WINDOWS
        os.contains("mac") || os.contains("darwin") -> OperatingSystem.MACOS
        os.contains("nux") || os.contains("nix") || os.contains("aix") -> OperatingSystem.LINUX
        else -> OperatingSystem.UNKNOWN
    }
}
