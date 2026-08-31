@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package io.github.kdroidfilter.ytdlpgui.nativeclipboard

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString
import platform.AppKit.NSPasteboardTypeURL
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.objc.class_getInstanceMethod
import platform.objc.method_getImplementation
import platform.objc.method_getTypeEncoding
import platform.objc.method_setImplementation
import platform.objc.objc_getClass
import platform.objc.sel_registerName
import kotlin.concurrent.Volatile

actual class NativeClipboardWatcher actual constructor() {
    actual fun start(onChanged: (String) -> Unit) = startClipboardListener(onChanged)

    actual fun stop() = stopClipboardListener()

    actual fun readText(): String = readClipboardTextNative()
}

private const val PASTEBOARD_CACHE_CLASS = "_CFPasteboardCache"
private const val SET_CHANGE_COUNT_SELECTOR = "setChangeCount:"

private typealias SetChangeCountProc = CFunction<(COpaquePointer?, COpaquePointer?, Int) -> Unit>

@Volatile
private var clipboardCallback: ((String) -> Unit)? = null

@Volatile
private var hookInstalled: Boolean = false

@Volatile
private var originalSetChangeCount: CPointer<SetChangeCountProc>? = null

private val callbackQueue: NSOperationQueue by lazy {
    NSOperationQueue().apply {
        name = "io.github.kdroidfilter.aerodl.clipboard"
        maxConcurrentOperationCount = 1
    }
}

private val hookedSetChangeCount = staticCFunction {
        self: COpaquePointer?,
        cmd: COpaquePointer?,
        changeCount: Int,
    ->
    originalSetChangeCount?.invoke(self, cmd, changeCount)
    scheduleClipboardCallback()
}

private fun startClipboardListener(onChanged: (String) -> Unit) {
    clipboardCallback = onChanged
    // Touch the general pasteboard so the pasteboard daemon tracks this process
    // and will send cache invalidations for copies that happen in other apps.
    NSPasteboard.generalPasteboard.changeCount
    if (!hookInstalled) {
        if (!installPasteboardHook()) {
            clipboardCallback = null
            throw IllegalStateException("Failed to install macOS pasteboard change hook")
        }
        hookInstalled = true
    }
}

private fun stopClipboardListener() {
    clipboardCallback = null
    callbackQueue.cancelAllOperations()
    callbackQueue.waitUntilAllOperationsAreFinished()
}

private fun installPasteboardHook(): Boolean {
    val cacheClass = objc_getClass(PASTEBOARD_CACHE_CLASS) as? ObjCClass ?: return false
    val selector = sel_registerName(SET_CHANGE_COUNT_SELECTOR)
    val method = class_getInstanceMethod(cacheClass, selector) ?: return false
    val encoding = method_getTypeEncoding(method)?.toKString() ?: return false
    if (!isSupportedSetChangeCountEncoding(encoding)) return false

    val previous = method_getImplementation(method) ?: return false
    originalSetChangeCount = previous.reinterpret()
    method_setImplementation(method, hookedSetChangeCount.reinterpret())
    return true
}

/**
 * `_CFPasteboardCache.setChangeCount:` is a private method whose ObjC type
 * encoding is `v20@0:8i16` on arm64 (void, int). Accept a small set of LP64
 * encodings so a future SDK bump that only changes frame size still works;
 * anything else falls back to polling.
 */
private fun isSupportedSetChangeCountEncoding(encoding: String): Boolean =
    encoding == "v20@0:8i16" || encoding == "v24@0:8i16" || encoding == "v24@0:8q16"

private fun scheduleClipboardCallback() {
    if (clipboardCallback == null) return
    callbackQueue.addOperationWithBlock {
        val callback = clipboardCallback ?: return@addOperationWithBlock
        // Re-dirty the process cache so the daemon keeps sending invalidations
        // for the next copy that happens outside this app.
        NSPasteboard.generalPasteboard.changeCount
        callback(readClipboardTextNative())
    }
}

private fun readClipboardTextNative(): String {
    val pasteboard = NSPasteboard.generalPasteboard
    repeat(5) { attempt ->
        val text = pasteboard.stringForType(NSPasteboardTypeString).orEmpty()
        if (text.isNotEmpty()) return text
        val url = pasteboard.stringForType(NSPasteboardTypeURL).orEmpty()
        if (url.isNotEmpty()) return url
        if (attempt < 4) {
            NSThread.sleepForTimeInterval(0.01 * (attempt + 1))
        }
    }
    return ""
}
