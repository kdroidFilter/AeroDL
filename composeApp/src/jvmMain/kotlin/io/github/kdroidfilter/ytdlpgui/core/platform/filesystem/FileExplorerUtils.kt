package io.github.kdroidfilter.ytdlpgui.core.platform.filesystem

import io.github.kdroidfilter.platformtools.LinuxDesktopEnvironment
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.detectLinuxDesktopEnvironment
import io.github.kdroidfilter.platformtools.getOperatingSystem
import java.io.File

object FileExplorerUtils {
    /**
     * Open the directory for the given path in the OS file manager and select the file when possible.
     * The path can be a file or a directory. Best-effort behavior with broad OS support.
     */
    fun openDirectoryForPath(path: String) {
        val target = File(path)
        val looksLikeFilePath = target.isFile || (!target.isDirectory && target.name.contains('.'))
        val fileToSelect: File? = when {
            target.isFile -> target
            looksLikeFilePath -> target
            else -> null
        }
        val dirToOpen: File? = when {
            target.isDirectory -> target
            fileToSelect != null -> target.parentFile
            else -> target.parentFile
        }
        fun runCommand(vararg cmd: String): Boolean = try {
            ProcessBuilder(*cmd).start(); true
        } catch (_: Throwable) { false }
        try {
            var handled = false
            if (fileToSelect != null) {
                val abs = fileToSelect.absolutePath
                handled = when (getOperatingSystem()) {
                    OperatingSystem.MACOS -> runCommand("open", "-R", abs)
                    OperatingSystem.WINDOWS -> runCommand("cmd", "/c", "explorer /select,\"$abs\"")
                    else -> {
                        var ok = false
                        when (detectLinuxDesktopEnvironment()) {
                            LinuxDesktopEnvironment.GNOME -> { ok = runCommand("nautilus", "--select", abs) }
                            LinuxDesktopEnvironment.KDE -> { ok = runCommand("dolphin", "--select", abs) }
                            LinuxDesktopEnvironment.XFCE -> { ok = runCommand("thunar", abs) }
                            LinuxDesktopEnvironment.CINNAMON -> { ok = runCommand("nemo", "--select", abs) }
                            LinuxDesktopEnvironment.MATE -> { ok = runCommand("caja", "--select", abs) }
                            LinuxDesktopEnvironment.UNKNOWN, null -> { }
                        }
                        if (!ok) {
                            val linuxAttempts: List<Array<String>> = listOf(
                                arrayOf("nautilus", "--select", abs),
                                arrayOf("dolphin", "--select", abs),
                                arrayOf("nemo", "--select", abs),
                                arrayOf("caja", "--select", abs),
                                arrayOf("thunar", abs),
                                arrayOf("pcmanfm", abs)
                            )
                            for (attempt in linuxAttempts) {
                                if (runCommand(*attempt)) { ok = true; break }
                            }
                        }
                        if (!ok) {
                            val dir = dirToOpen
                            if (dir != null) { ok = runCommand("xdg-open", dir.absolutePath) }
                        }
                        ok
                    }
                }
            }
            if (!handled) {
                val dir = dirToOpen
                if (dir != null && dir.exists()) {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        runCatching { java.awt.Desktop.getDesktop().open(dir) }
                            .onSuccess { handled = true }
                    }
                    if (!handled) {
                        handled = when (getOperatingSystem()) {
                            OperatingSystem.WINDOWS -> runCommand("explorer.exe", dir.absolutePath)
                            OperatingSystem.MACOS -> runCommand("open", dir.absolutePath)
                            else -> runCommand("xdg-open", dir.absolutePath)
                        }
                    }
                }
            }
        } catch (_: Throwable) { /* ignore */ }
    }
}
