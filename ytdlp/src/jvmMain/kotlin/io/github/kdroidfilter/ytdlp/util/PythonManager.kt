package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.path
import java.io.File

/**
 * Manages Python standalone installation for macOS.
 * On macOS, we use Python standalone + yt-dlp script instead of PyInstaller binary
 * to avoid Gatekeeper slowness (0.3s vs 10s per call).
 */
object PythonManager {

    private val pythonDir = File(FileKit.databasesDir.path, "python")
    private val ytdlpScriptPath = File(FileKit.databasesDir.path, "yt-dlp").absolutePath

    /**
     * Check if Python is available and properly installed.
     */
    fun isPythonAvailable(): Boolean {
        val pythonExe = getPythonExecutable()
        val file = File(pythonExe)
        if (!file.exists()) return false

        // Verify it can execute
        return try {
            val process = ProcessBuilder(pythonExe, "--version")
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the path to the Python executable.
     */
    fun getPythonExecutable(): String {
        return File(pythonDir, "python/bin/python3.12").absolutePath
    }

    /**
     * Get the path to the yt-dlp script.
     */
    fun getYtDlpScriptPath(): String = ytdlpScriptPath

    /**
     * Download and install Python standalone for the current architecture.
     */
    suspend fun downloadPython(
        manifest: ReleaseManifest,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean {
        val os = getOperatingSystem()
        if (os != OperatingSystem.MACOS) {
            errorln { "Python download is only supported on macOS" }
            return false
        }

        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val archToken = when {
            arch.contains("aarch64") || arch.contains("arm64") -> "aarch64-apple-darwin"
            else -> "x86_64-apple-darwin"
        }

        debugln { "Downloading Python for architecture token: $archToken" }

        val pythonRelease = manifest.releases.python
        if (pythonRelease == null) {
            errorln { "Python release info not found in manifest" }
            return false
        }

        val asset = pythonRelease.assets.find {
            it.name.contains(archToken) &&
                it.name.contains("install_only") &&
                it.name.endsWith(".tar.gz")
        }
        if (asset == null) {
            errorln {
                "Python asset not found for $archToken. Available assets: ${
                    pythonRelease.assets.joinToString(", ") { it.name }
                }"
            }
            return false
        }

        return try {
            pythonDir.mkdirs()
            val tempFile = File.createTempFile("python", ".tar.gz")

            debugln { "Downloading Python from: ${asset.browserDownloadUrl}" }
            PlatformUtils.downloadFile(asset.browserDownloadUrl, tempFile, onProgress)

            debugln { "Extracting Python to: $pythonDir" }
            extractTarGz(tempFile, pythonDir)
            tempFile.delete()

            debugln { "Python installed successfully" }
            true
        } catch (e: Exception) {
            errorln(e) { "Failed to download Python: ${e.message}" }
            false
        }
    }

    /**
     * Download the yt-dlp pure Python script.
     */
    suspend fun downloadYtDlpScript(
        manifest: ReleaseManifest,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean {
        val scriptRelease = manifest.releases.ytDlpScript
        if (scriptRelease == null) {
            errorln { "yt-dlp script release info not found in manifest" }
            return false
        }

        // The script asset should be named just "yt-dlp" (no extension)
        val asset = scriptRelease.assets.find { it.name == "yt-dlp" }
        if (asset == null) {
            errorln { "yt-dlp script asset not found" }
            return false
        }

        return try {
            val destFile = File(ytdlpScriptPath)
            destFile.parentFile?.mkdirs()

            debugln { "Downloading yt-dlp script from: ${asset.browserDownloadUrl}" }
            PlatformUtils.downloadFile(asset.browserDownloadUrl, destFile, onProgress)

            // Make executable
            PlatformUtils.makeExecutable(destFile)

            debugln { "yt-dlp script downloaded successfully" }
            true
        } catch (e: Exception) {
            errorln(e) { "Failed to download yt-dlp script: ${e.message}" }
            false
        }
    }

    /**
     * Extract a tar.gz file to a directory.
     */
    private fun extractTarGz(tarGzFile: File, destDir: File) {
        destDir.mkdirs()
        val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("Failed to extract tar.gz (exit code: $exitCode): $output")
        }
    }

    /**
     * Check if Python needs to be downloaded/updated.
     */
    fun needsPythonDownload(): Boolean {
        return !isPythonAvailable()
    }

    /**
     * Check if yt-dlp script needs to be downloaded/updated.
     */
    fun needsYtDlpScriptDownload(): Boolean {
        return !File(ytdlpScriptPath).exists()
    }
}
