package io.github.kdroidfilter.ytdlpgui.nativeclipboard

internal actual fun startClipboardListener(onChanged: (String) -> Unit) {
    throw UnsupportedOperationException("Windows clipboard listener is only available on Windows")
}

internal actual fun stopClipboardListener() = Unit

internal actual fun readClipboardText(): String = ""
