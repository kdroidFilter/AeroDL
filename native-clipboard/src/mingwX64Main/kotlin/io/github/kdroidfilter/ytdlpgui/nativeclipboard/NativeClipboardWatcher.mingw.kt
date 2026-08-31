@file:OptIn(ExperimentalForeignApi::class)

package io.github.kdroidfilter.ytdlpgui.nativeclipboard

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.plus
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.wcstr
import platform.windows.CF_UNICODETEXT
import platform.windows.CreateEventW
import platform.windows.CreateThread
import platform.windows.CreateWindowExW
import platform.windows.CloseClipboard
import platform.windows.CloseHandle
import platform.windows.DefWindowProcW
import platform.windows.DestroyWindow
import platform.windows.DispatchMessageW
import platform.windows.ERROR_CLASS_ALREADY_EXISTS
import platform.windows.GetClipboardData
import platform.windows.GetLastError
import platform.windows.GetMessageW
import platform.windows.GetModuleHandleW
import platform.windows.GetProcAddress
import platform.windows.LoadLibraryW
import platform.windows.GlobalLock
import platform.windows.GlobalUnlock
import platform.windows.HWND
import platform.windows.IsClipboardFormatAvailable
import platform.windows.LPARAM
import platform.windows.LPVOID
import platform.windows.LRESULT
import platform.windows.MSG
import platform.windows.OpenClipboard
import platform.windows.PostMessageW
import platform.windows.PostQuitMessage
import platform.windows.RegisterClassExW
import platform.windows.SetEvent
import platform.windows.Sleep
import platform.windows.TranslateMessage
import platform.windows.UINT
import platform.windows.WaitForSingleObject
import platform.windows.WM_CLOSE
import platform.windows.WM_DESTROY
import platform.windows.WNDCLASSEXW
import platform.windows.WPARAM
import platform.windows.WS_OVERLAPPED
import kotlin.concurrent.Volatile

actual class NativeClipboardWatcher actual constructor() {
    actual fun start(onChanged: (String) -> Unit) = startClipboardListener(onChanged)

    actual fun stop() = stopClipboardListener()

    actual fun readText(): String = readClipboardTextNative()
}

private const val WM_CLIPBOARDUPDATE: Int = 0x031D
private const val READY_TIMEOUT_MS: UInt = 8_000u
private const val STOP_TIMEOUT_MS: UInt = 5_000u

@Suppress("UNCHECKED_CAST")
private val hwndMessage: HWND? = interpretCPointer<CPointed>(nativeNullPtr + (-3L)) as HWND?

@Volatile
private var clipboardCallback: ((String) -> Unit)? = null

@Volatile
private var listenerHwnd: HWND? = null

private var threadHandle: COpaquePointer? = null

private var readyEvent: COpaquePointer? = null

@Volatile
private var startSucceeded: Boolean = false

@Volatile
private var startError: String? = null

private typealias ClipboardListenerFn = CFunction<(HWND?) -> Int>

private val clipboardThreadProc = staticCFunction { _: LPVOID? ->
    runClipboardMessageLoop()
    0u
}

private fun startClipboardListener(onChanged: (String) -> Unit) {
    if (threadHandle != null) return

    clipboardCallback = onChanged
    startSucceeded = false
    startError = null
    readyEvent = CreateEventW(null, 1, 0, null)

    threadHandle = CreateThread(
        null,
        0u,
        clipboardThreadProc,
        null,
        0u,
        null,
    )
    if (threadHandle == null) {
        clipboardCallback = null
        val error = GetLastError()
        readyEvent?.let { CloseHandle(it) }
        readyEvent = null
        throw IllegalStateException("CreateThread failed: $error")
    }

    WaitForSingleObject(readyEvent, READY_TIMEOUT_MS)
    if (!startSucceeded) {
        val error = startError ?: "Failed to start Windows clipboard listener"
        stopClipboardListener()
        throw IllegalStateException(error)
    }
}

private fun stopClipboardListener() {
    clipboardCallback = null
    val hwnd = listenerHwnd
    if (hwnd != null) {
        PostMessageW(hwnd, WM_CLOSE.toUInt(), 0u, 0L)
    }
    threadHandle?.let {
        WaitForSingleObject(it, STOP_TIMEOUT_MS)
        CloseHandle(it)
    }
    threadHandle = null
    listenerHwnd = null
    readyEvent?.let { CloseHandle(it) }
    readyEvent = null
    startSucceeded = false
    startError = null
}

private fun runClipboardMessageLoop() {
    memScoped {
        val className = "AeroDLClipboardListener"
        val hInstance = GetModuleHandleW(null)
        val wc = alloc<WNDCLASSEXW>()
        wc.cbSize = sizeOf<WNDCLASSEXW>().toUInt()
        wc.style = 0u
        wc.lpfnWndProc = staticCFunction(::clipboardWndProc)
        wc.cbClsExtra = 0
        wc.cbWndExtra = 0
        wc.hInstance = hInstance
        wc.hIcon = null
        wc.hCursor = null
        wc.hbrBackground = null
        wc.lpszMenuName = null
        wc.lpszClassName = className.wcstr.ptr
        wc.hIconSm = null

        if (RegisterClassExW(wc.ptr) == 0u.toUShort()) {
            val error = GetLastError()
            if (error != ERROR_CLASS_ALREADY_EXISTS.toUInt()) {
                failStart("RegisterClassExW failed: $error")
                return
            }
        }

        val hwnd = CreateWindowExW(
            0u,
            className,
            className,
            WS_OVERLAPPED.toUInt(),
            0,
            0,
            0,
            0,
            hwndMessage,
            null,
            hInstance,
            null,
        )
        if (hwnd == null) {
            failStart("CreateWindowExW failed: ${GetLastError()}")
            return
        }
        listenerHwnd = hwnd

        val addListener = loadClipboardListenerProc("AddClipboardFormatListener")
        if (addListener == null) {
            DestroyWindow(hwnd)
            listenerHwnd = null
            failStart("AddClipboardFormatListener is unavailable")
            return
        }
        if (addListener(hwnd) == 0) {
            val error = GetLastError()
            DestroyWindow(hwnd)
            listenerHwnd = null
            failStart("AddClipboardFormatListener failed: $error")
            return
        }

        startSucceeded = true
        readyEvent?.let { SetEvent(it) }

        val msg = alloc<MSG>()
        while (GetMessageW(msg.ptr, null, 0u, 0u) > 0) {
            TranslateMessage(msg.ptr)
            DispatchMessageW(msg.ptr)
        }
    }
}

private fun clipboardWndProc(
    hwnd: HWND?,
    msg: UINT,
    wParam: WPARAM,
    lParam: LPARAM,
): LRESULT {
    when (msg) {
        WM_CLIPBOARDUPDATE.toUInt() -> {
            val text = readClipboardTextNative()
            clipboardCallback?.invoke(text)
            return 0
        }
        WM_CLOSE.toUInt() -> {
            DestroyWindow(hwnd)
            return 0
        }
        WM_DESTROY.toUInt() -> {
            loadClipboardListenerProc("RemoveClipboardFormatListener")?.invoke(hwnd)
            listenerHwnd = null
            PostQuitMessage(0)
            return 0
        }
    }
    return DefWindowProcW(hwnd, msg, wParam, lParam)
}

private fun loadClipboardListenerProc(name: String): CPointer<ClipboardListenerFn>? {
    val user32 = GetModuleHandleW("user32.dll")
        ?: GetModuleHandleW("user32")
        ?: LoadLibraryW("user32.dll")
        ?: return null
    return GetProcAddress(user32, name)?.reinterpret()
}

private fun readClipboardTextNative(): String {
    for (attempt in 0 until 10) {
        if (OpenClipboard(null) == 0) {
            Sleep((10u * (attempt.toUInt() + 1u)))
            continue
        }
        try {
            if (IsClipboardFormatAvailable(CF_UNICODETEXT.toUInt()) == 0) return ""
            val handle = GetClipboardData(CF_UNICODETEXT.toUInt()) ?: return ""
            val locked = GlobalLock(handle) ?: return ""
            try {
                return locked.reinterpret<UShortVar>().toKStringFromUtf16()
            } finally {
                GlobalUnlock(handle)
            }
        } finally {
            CloseClipboard()
        }
    }
    return ""
}

private fun failStart(message: String) {
    startSucceeded = false
    startError = message
    readyEvent?.let { SetEvent(it) }
}
