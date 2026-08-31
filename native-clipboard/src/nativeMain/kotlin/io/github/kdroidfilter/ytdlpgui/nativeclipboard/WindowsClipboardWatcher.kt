package io.github.kdroidfilter.ytdlpgui.nativeclipboard

/**
 * Win32 clipboard format listener, called from the JVM via Nucleus Native Access.
 *
 * On Windows this creates a message-only window, registers
 * [AddClipboardFormatListener](https://learn.microsoft.com/windows/win32/api/winuser/nf-winuser-addclipboardformatlistener),
 * and invokes [onChanged] on each `WM_CLIPBOARDUPDATE`. Other platforms throw from [start].
 */
class WindowsClipboardWatcher {
    fun start(onChanged: (String) -> Unit) {
        startClipboardListener(onChanged)
    }

    fun stop() {
        stopClipboardListener()
    }

    fun readText(): String = readClipboardText()
}

internal expect fun startClipboardListener(onChanged: (String) -> Unit)

internal expect fun stopClipboardListener()

internal expect fun readClipboardText(): String
