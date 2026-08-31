package io.github.kdroidfilter.ytdlpgui.nativeclipboard

/**
 * Native clipboard watcher, called from the JVM via Nucleus Native Access.
 *
 * - Windows: message-only window + `AddClipboardFormatListener` / `WM_CLIPBOARDUPDATE`
 * - macOS: `_CFPasteboardCache.setChangeCount:` hook (Chromium's approach)
 * - elsewhere: [start] throws so the JVM can fall back to polling
 */
expect class NativeClipboardWatcher() {
    fun start(onChanged: (String) -> Unit)
    fun stop()
    fun readText(): String
}
