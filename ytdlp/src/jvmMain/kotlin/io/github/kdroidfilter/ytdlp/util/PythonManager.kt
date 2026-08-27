package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.path
import java.io.File

/**
 * Manages a pinned Python standalone install plus the yt-dlp pure-Python script.
 *
 * Python is downloaded once (fixed version from the release manifest) and is never
 * auto-updated. Only the yt-dlp script is refreshed when a newer release is available.
 * This avoids PyInstaller/Gatekeeper overhead on every invocation.
 */
object PythonManager {

    private fun dataDir(): File {
        return try {
            File(FileKit.databasesDir.path)
        } catch (_: Exception) {
            File(System.getProperty("java.io.tmpdir"), "aerodl")
        }
    }

    private val pythonDir: File
        get() = File(dataDir(), "python")

    private val ytdlpScriptPath: String
        get() = File(dataDir(), "yt-dlp").absolutePath

    fun isPythonAvailable(): Boolean {
        val pythonExe = getPythonExecutable()
        val file = File(pythonExe)
        if (!file.exists()) return false

        return try {
            val process = ProcessBuilder(pythonExe, "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun getPythonExecutable(): String {
        val os = getOperatingSystem()
        return when (os) {
            OperatingSystem.WINDOWS -> File(pythonDir, "python/python.exe").absolutePath
            else -> File(pythonDir, "python/bin/python3.12").absolutePath
        }
    }

    fun getYtDlpScriptPath(): String = ytdlpScriptPath

    /**
     * Prefix a yt-dlp invocation so it always runs through the bundled Python:
     * `python <script> [args...]`
     */
    fun command(scriptPath: String, args: List<String> = emptyList()): List<String> =
        buildList {
            add(getPythonExecutable())
            add(scriptPath)
            addAll(args)
        }

    suspend fun downloadPython(
        manifest: ReleaseManifest,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean {
        val archToken = pythonArchToken()
        debugln { "Downloading Python for architecture token: $archToken" }

        val pythonRelease = manifest.releases.python
        if (pythonRelease == null) {
            errorln { "Python release info not found in manifest" }
            return false
        }

        val asset = pythonRelease.assets.find { asset ->
            asset.name.contains(archToken) &&
                asset.name.endsWith("-install_only.tar.gz") &&
                !asset.name.contains("stripped")
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

            val pythonExe = File(getPythonExecutable())
            if (!pythonExe.exists()) {
                errorln { "Python executable missing after extraction: ${pythonExe.absolutePath}" }
                return false
            }
            if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                PlatformUtils.makeExecutable(pythonExe)
            }

            debugln { "Python installed successfully" }
            true
        } catch (e: Exception) {
            errorln(e) { "Failed to download Python: ${e.message}" }
            false
        }
    }

    suspend fun downloadYtDlpScript(
        manifest: ReleaseManifest,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): Boolean {
        val scriptRelease = manifest.releases.ytDlpScript ?: manifest.releases.ytDlp

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

            if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                PlatformUtils.makeExecutable(destFile)
            }

            debugln { "yt-dlp script downloaded successfully" }
            true
        } catch (e: Exception) {
            errorln(e) { "Failed to download yt-dlp script: ${e.message}" }
            false
        }
    }

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

    fun needsPythonDownload(): Boolean = !isPythonAvailable()

    fun needsYtDlpScriptDownload(): Boolean = !File(ytdlpScriptPath).exists()

    internal fun pythonArchToken(): String {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64")
        return when (os) {
            OperatingSystem.MACOS -> if (isArm64) "aarch64-apple-darwin" else "x86_64-apple-darwin"
            OperatingSystem.WINDOWS -> if (isArm64) "aarch64-pc-windows-msvc" else "x86_64-pc-windows-msvc"
            OperatingSystem.LINUX -> {
                val libc = if (isMuslLinux()) "musl" else "gnu"
                val cpu = if (isArm64) "aarch64" else "x86_64"
                "$cpu-unknown-linux-$libc"
            }
            else -> if (isArm64) "aarch64-unknown-linux-gnu" else "x86_64-unknown-linux-gnu"
        }
    }

    private fun isMuslLinux(): Boolean {
        if (File("/etc/alpine-release").exists()) return true
        return try {
            val process = ProcessBuilder("ldd", "--version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("musl")
        } catch (_: Exception) {
            false
        }
    }
}
