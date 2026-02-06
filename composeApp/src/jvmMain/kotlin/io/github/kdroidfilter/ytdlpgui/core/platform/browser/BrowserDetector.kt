package io.github.kdroidfilter.ytdlpgui.core.platform.browser

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import java.io.File

/**
 * Supported browsers for cookie extraction.
 */
enum class SupportedBrowser(val value: String) {
    FIREFOX("firefox"),
    CHROME("chrome"),
    SAFARI("safari"),
    EDGE("edge"),
    BRAVE("brave"),
    OPERA("opera"),
    VIVALDI("vivaldi"),
    CHROMIUM("chromium");
}

/**
 * Detects which browsers are installed on the current system.
 */
object BrowserDetector {

    private val detectionCache: MutableMap<SupportedBrowser, Boolean> = mutableMapOf()

    /**
     * Browsers that are sandboxed on Windows and cannot be used for cookie extraction.
     * Chrome and Edge use a sandbox that prevents yt-dlp from accessing their cookies.
     */
    private val windowsSandboxedBrowsers = setOf(
        SupportedBrowser.CHROME,
        SupportedBrowser.EDGE
    )

    /**
     * Returns a list of browsers that are installed on the current system
     * and can be used for cookie extraction.
     * On Windows, Chrome and Edge are excluded because they are sandboxed.
     */
    fun getInstalledBrowsers(): List<SupportedBrowser> {
        val isWindows = getOperatingSystem() == OperatingSystem.WINDOWS
        return SupportedBrowser.entries.filter { browser ->
            val isInstalled = isInstalled(browser)
            val isUsable = !isWindows || browser !in windowsSandboxedBrowsers
            isInstalled && isUsable
        }
    }

    /**
     * Checks if a specific browser is installed.
     */
    fun isInstalled(browser: SupportedBrowser): Boolean {
        return detectionCache.getOrPut(browser) {
            when (getOperatingSystem()) {
                OperatingSystem.MACOS -> isBrowserInstalledMacOS(browser)
                OperatingSystem.WINDOWS -> isBrowserInstalledWindows(browser)
                else -> isBrowserInstalledLinux(browser)
            }
        }
    }

    /**
     * Clears the detection cache, forcing re-detection on next call.
     */
    fun clearCache() {
        detectionCache.clear()
    }

    private fun isBrowserInstalledMacOS(browser: SupportedBrowser): Boolean {
        val appPaths = when (browser) {
            SupportedBrowser.FIREFOX -> listOf(
                "/Applications/Firefox.app",
                "${System.getProperty("user.home")}/Applications/Firefox.app"
            )
            SupportedBrowser.CHROME -> listOf(
                "/Applications/Google Chrome.app",
                "${System.getProperty("user.home")}/Applications/Google Chrome.app"
            )
            SupportedBrowser.SAFARI -> listOf("/Applications/Safari.app")
            SupportedBrowser.EDGE -> listOf(
                "/Applications/Microsoft Edge.app",
                "${System.getProperty("user.home")}/Applications/Microsoft Edge.app"
            )
            SupportedBrowser.BRAVE -> listOf(
                "/Applications/Brave Browser.app",
                "${System.getProperty("user.home")}/Applications/Brave Browser.app"
            )
            SupportedBrowser.OPERA -> listOf(
                "/Applications/Opera.app",
                "${System.getProperty("user.home")}/Applications/Opera.app"
            )
            SupportedBrowser.VIVALDI -> listOf(
                "/Applications/Vivaldi.app",
                "${System.getProperty("user.home")}/Applications/Vivaldi.app"
            )
            SupportedBrowser.CHROMIUM -> listOf(
                "/Applications/Chromium.app",
                "${System.getProperty("user.home")}/Applications/Chromium.app"
            )
        }
        return appPaths.any { File(it).exists() }
    }

    private fun isBrowserInstalledWindows(browser: SupportedBrowser): Boolean {
        val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
        val programFilesX86 = System.getenv("ProgramFiles(x86)") ?: "C:\\Program Files (x86)"
        val localAppData = System.getenv("LOCALAPPDATA") ?: "${System.getProperty("user.home")}\\AppData\\Local"

        val paths = when (browser) {
            SupportedBrowser.FIREFOX -> listOf(
                "$programFiles\\Mozilla Firefox\\firefox.exe",
                "$programFilesX86\\Mozilla Firefox\\firefox.exe"
            )
            SupportedBrowser.CHROME -> listOf(
                "$programFiles\\Google\\Chrome\\Application\\chrome.exe",
                "$programFilesX86\\Google\\Chrome\\Application\\chrome.exe",
                "$localAppData\\Google\\Chrome\\Application\\chrome.exe"
            )
            SupportedBrowser.SAFARI -> emptyList() // Safari not available on Windows
            SupportedBrowser.EDGE -> listOf(
                "$programFiles\\Microsoft\\Edge\\Application\\msedge.exe",
                "$programFilesX86\\Microsoft\\Edge\\Application\\msedge.exe"
            )
            SupportedBrowser.BRAVE -> listOf(
                "$programFiles\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
                "$programFilesX86\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
                "$localAppData\\BraveSoftware\\Brave-Browser\\Application\\brave.exe"
            )
            SupportedBrowser.OPERA -> listOf(
                "$programFiles\\Opera\\launcher.exe",
                "$programFilesX86\\Opera\\launcher.exe",
                "$localAppData\\Programs\\Opera\\launcher.exe"
            )
            SupportedBrowser.VIVALDI -> listOf(
                "$programFiles\\Vivaldi\\Application\\vivaldi.exe",
                "$localAppData\\Vivaldi\\Application\\vivaldi.exe"
            )
            SupportedBrowser.CHROMIUM -> listOf(
                "$programFiles\\Chromium\\Application\\chrome.exe",
                "$localAppData\\Chromium\\Application\\chrome.exe"
            )
        }
        return paths.any { File(it).exists() }
    }

    private fun isBrowserInstalledLinux(browser: SupportedBrowser): Boolean {
        val executableNames = when (browser) {
            SupportedBrowser.FIREFOX -> listOf("firefox", "firefox-esr")
            SupportedBrowser.CHROME -> listOf("google-chrome", "google-chrome-stable")
            SupportedBrowser.SAFARI -> emptyList() // Safari not available on Linux
            SupportedBrowser.EDGE -> listOf("microsoft-edge", "microsoft-edge-stable")
            SupportedBrowser.BRAVE -> listOf("brave-browser", "brave")
            SupportedBrowser.OPERA -> listOf("opera")
            SupportedBrowser.VIVALDI -> listOf("vivaldi", "vivaldi-stable")
            SupportedBrowser.CHROMIUM -> listOf("chromium", "chromium-browser")
        }

        if (executableNames.isEmpty()) return false

        // Check common binary locations
        val commonPaths = listOf(
            "/usr/bin",
            "/usr/local/bin",
            "/snap/bin",
            "/var/lib/flatpak/exports/bin",
            "${System.getProperty("user.home")}/.local/bin"
        )

        for (name in executableNames) {
            for (path in commonPaths) {
                if (File("$path/$name").exists()) {
                    return true
                }
            }
        }

        // Fallback: try 'which' command
        return executableNames.any { name ->
            try {
                val process = ProcessBuilder("which", name)
                    .redirectErrorStream(true)
                    .start()
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (_: Exception) {
                false
            }
        }
    }
}
