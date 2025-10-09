package io.github.kdroidfilter.ytdlpgui.core.platform.browser

import java.awt.Desktop
import java.net.URI

fun openUrlInBrowser(url: String) {
    try {
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI(url))
        }
    } catch (_: Exception) {
        // no-op: ignore failures to open browser
    }
}