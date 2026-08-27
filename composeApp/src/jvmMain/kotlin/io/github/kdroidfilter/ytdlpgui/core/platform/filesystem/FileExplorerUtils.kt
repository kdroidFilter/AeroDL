package io.github.kdroidfilter.ytdlpgui.core.platform.filesystem

import dev.nucleusframework.core.runtime.LinuxDesktopEnvironment
import dev.nucleusframework.core.runtime.Platform
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
                handled = when (Platform.Current) {
                    Platform.MacOS -> runCommand("open", "-R", abs)
                    Platform.Windows -> runCommand("cmd", "/c", "explorer /select,\"$abs\"")
                    else -> {
                        var ok = false
                        when (LinuxDesktopEnvironment.Current) {
                            LinuxDesktopEnvironment.Gnome -> { ok = runCommand("nautilus", "--select", abs) }
                            LinuxDesktopEnvironment.KDE -> { ok = runCommand("dolphin", "--select", abs) }
                            LinuxDesktopEnvironment.XFCE -> { ok = runCommand("thunar", abs) }
                            LinuxDesktopEnvironment.Cinnamon -> { ok = runCommand("nemo", "--select", abs) }
                            LinuxDesktopEnvironment.Mate -> { ok = runCommand("caja", "--select", abs) }
                            LinuxDesktopEnvironment.Unknown -> { }
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
                    handled = when (Platform.Current) {
                        Platform.Windows -> runCommand("explorer.exe", dir.absolutePath)
                        Platform.MacOS -> runCommand("open", dir.absolutePath)
                        else -> runCommand("xdg-open", dir.absolutePath)
                    }
                }
            }
        } catch (_: Throwable) { /* ignore */ }
    }
}
