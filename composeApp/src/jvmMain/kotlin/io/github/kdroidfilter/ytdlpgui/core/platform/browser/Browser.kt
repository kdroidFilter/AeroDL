package io.github.kdroidfilter.ytdlpgui.core.platform.browser

import dev.nucleusframework.core.runtime.Platform
import java.awt.Desktop
import java.net.URI

fun openUrlInBrowser(url: String) {
    try {
        // Prefer an OS subprocess over AWT Desktop.browse(). On GNOME, browse() goes
        // through in-process gtk_show_uri/gio and can crash when mixed with a native GTK UI.
        if (openWithOsCommand(url)) return
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI(url))
        }
    } catch (_: Exception) {
        // no-op: ignore failures to open browser
    }
}

private fun openWithOsCommand(url: String): Boolean {
    val command = when (Platform.Current) {
        Platform.Linux -> arrayOf("xdg-open", url)
        Platform.MacOS -> arrayOf("open", url)
        Platform.Windows -> arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
        else -> return false
    }
    return try {
        ProcessBuilder(*command).start()
        true
    } catch (_: Exception) {
        false
    }
}
