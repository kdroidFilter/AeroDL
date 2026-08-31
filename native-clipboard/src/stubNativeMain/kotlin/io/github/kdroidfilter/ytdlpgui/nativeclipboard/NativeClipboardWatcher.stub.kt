package io.github.kdroidfilter.ytdlpgui.nativeclipboard

actual class NativeClipboardWatcher actual constructor() {
    actual fun start(onChanged: (String) -> Unit) {
        throw UnsupportedOperationException("Native clipboard listener is not available on this platform")
    }

    actual fun stop() = Unit

    actual fun readText(): String = ""
}
