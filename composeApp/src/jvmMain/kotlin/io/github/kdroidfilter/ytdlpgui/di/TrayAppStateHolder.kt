package io.github.kdroidfilter.ytdlpgui.di

import dev.nucleusframework.composenativetray.trayapp.TrayAppState

/**
 * Global holder to ensure a single TrayAppState instance is shared across the app.
 *
 * - DI providers should call getOrCreate() to obtain the shared instance.
 * - The Compose entry point (main.kt) should set(...) the instance created by rememberTrayAppState(...)
 *   so both UI and non-UI consumers (e.g., DownloadManager) use the very same instance.
 */
object TrayAppStateHolder {
    @Volatile
    private var _instance: TrayAppState? = null

    fun set(state: TrayAppState) {
        _instance = state
    }

    fun getOrCreate(): TrayAppState {
        _instance?.let { return it }
        synchronized(this) {
            _instance?.let { return it }
            val created = TrayAppState()
            _instance = created
            return created
        }
    }
}
