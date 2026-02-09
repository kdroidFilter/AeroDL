package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.network.HttpsConnectionFactory
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.errorln
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

object PlatformUtils {
    private val ffmpegInstallMutex = Mutex()

    // --- yt-dlp ---

    private fun getDataDir(): String = FileKit.databasesDir.path

    fun getDefaultBinaryPath(): String {
        val dir = getDataDir()
        val os = getOperatingSystem()
        val binaryName = when (os) {
            OperatingSystem.WINDOWS -> "yt-dlp.exe"
            else -> "yt-dlp"
        }
        return File(dir, binaryName).absolutePath
    }

    /**
     * Resolve the official asset name for yt-dlp on this platform.
     *
     * NOTE (macOS): KEEPING UNIVERSAL BINARY NAME. Do NOT introduce arm64-specific variant.
     *               The correct asset is still "yt-dlp_macos".
     */
    suspend fun getYtDlpAssetNameForSystem(): String {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()

        return when (os) {
            OperatingSystem.WINDOWS -> when {
                arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_win_arm64.exe"
                arch.contains("32") || (arch.contains("x86") && !arch.contains("64")) -> "yt-dlp_x86.exe"
                else -> "yt-dlp.exe"
            }
            // macOS stays universal
            OperatingSystem.MACOS -> "yt-dlp_macos"
            OperatingSystem.LINUX -> {
                val isMusl = isMusl()
                when {
                    isMusl && (arch.contains("aarch64") || arch.contains("arm64")) -> "yt-dlp_musllinux_aarch64"
                    isMusl -> "yt-dlp_musllinux"
                    arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_linux_aarch64"
                    arch.contains("armv7") -> "yt-dlp_linux_armv7l"
                    else -> "yt-dlp_linux"
                }
            }
            else -> "yt-dlp"
        }
    }

    private suspend fun isMusl(): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = ProcessBuilder("ldd", "--version").redirectErrorStream(true).start()
            val exited = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(1500) { p.waitFor() } }
            if (exited == null) {
                p.destroyForcibly()
                return@withContext false
            }
            val out = p.inputStream.bufferedReader().readText()
            out.contains("musl")
        } catch (e: Exception) {
            debugln { "ldd check failed, assuming not musl. Error: ${e.message}" }
            false
        }
    }

    /**
     * Robust downloader with timeouts and progress callback.
     * Keeps memory usage small by streaming to disk.
     */
    suspend fun downloadFile(
        url: String,
        dest: File,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()

        val uri = java.net.URI.create(url)
        val conn = HttpsConnectionFactory.openConnection(uri.toURL()) {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "kdroidFilter-ytdlp/1.0")
        }

        val total = conn.getHeaderFieldLong("Content-Length", -1L).takeIf { it > 0 }
        conn.inputStream.use { input ->
            Channels.newChannel(input).use { rch ->
                java.io.FileOutputStream(dest).use { fos ->
                    val buffer = ByteBuffer.allocateDirect(1024 * 64) // 64KB
                    var readTotal = 0L
                    while (isActive) {
                        buffer.clear()
                        val n = rch.read(buffer)
                        if (n <= 0) break
                        buffer.flip()
                        fos.channel.write(buffer)
                        readTotal += n
                        onProgress?.invoke(readTotal, total)
                    }
                }
            }
        }
    }

    /**
     * Mark file as executable and remove macOS quarantine if needed.
     */
    suspend fun makeExecutable(file: File) = withContext(Dispatchers.IO) {
        try {
            val path = file.toPath()
            val perms = Files.getPosixFilePermissions(path).toMutableSet()
            perms.add(PosixFilePermission.OWNER_EXECUTE)
            perms.add(PosixFilePermission.GROUP_EXECUTE)
            perms.add(PosixFilePermission.OTHERS_EXECUTE)
            Files.setPosixFilePermissions(path, perms)

            if (getOperatingSystem() == OperatingSystem.MACOS) {
                try {
                    ProcessBuilder("xattr", "-d", "com.apple.quarantine", file.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor()
                } catch (e: Exception) {
                    debugln { "Could not remove quarantine attribute from ${file.name}: ${e.message}" }
                }
                // Note: We don't codesign yt-dlp because it's a PyInstaller bundle with embedded
                // Python.framework, and codesigning with --options runtime breaks the embedded components.
                // Quarantine removal above is sufficient for allowing execution.
                debugln { "✅ Made ${file.name} executable and removed quarantine" }
            }

        } catch (_: UnsupportedOperationException) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
        }
    }

    // --- Deno (JavaScript runtime for yt-dlp) ---

    fun getDefaultDenoPath(): String {
        val dir = File(getDataDir(), "deno")
        val exe = if (getOperatingSystem() == OperatingSystem.WINDOWS) "deno.exe" else "deno"
        return File(dir, exe).absolutePath
    }

    suspend fun findDenoInSystemPath(): String? = withContext(Dispatchers.IO) {
        val cmd = if (getOperatingSystem() == OperatingSystem.WINDOWS) listOf("where", "deno") else listOf("which", "deno")
        try {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exited = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(1500) { p.waitFor() } }
            if (exited == null) {
                p.destroyForcibly(); return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull()?.trim() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun denoVersion(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val p = ProcessBuilder(listOf(path, "--version")).redirectErrorStream(true).start()
            val exited = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(2_000) { p.waitFor() } }
            if (exited == null) {
                p.destroyForcibly(); return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull() else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Select the appropriate Deno asset name for the current system.
     * Returns the exact asset name from denoland/deno GitHub releases.
     * Note: Windows ARM64 is not available, returns null in that case.
     */
    fun getDenoAssetNameForSystem(): String? {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64")

        return when (os) {
            OperatingSystem.WINDOWS -> when {
                isArm64 -> null // Windows ARM64 not available for Deno
                else -> "deno-x86_64-pc-windows-msvc.zip"
            }
            OperatingSystem.LINUX -> if (isArm64) "deno-aarch64-unknown-linux-gnu.zip" else "deno-x86_64-unknown-linux-gnu.zip"
            OperatingSystem.MACOS -> if (isArm64) "deno-aarch64-apple-darwin.zip" else "deno-x86_64-apple-darwin.zip"
            else -> null
        }
    }

    /**
     * Download and install Deno in the app cache, verifying it runs.
     *
     * @param assetName Expected archive file name (e.g. "deno-x86_64-unknown-linux-gnu.zip").
     * @param forceDownload Re-download even if a working binary already exists.
     * @param downloadUrl Direct download URL for the archive.
     * @param onProgress Optional progress callback.
     */
    suspend fun downloadAndInstallDeno(
        assetName: String,
        forceDownload: Boolean,
        downloadUrl: String,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        val denoDir = File(getDataDir(), "deno")
        val targetDeno = File(denoDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "deno.exe" else "deno")

        if (!forceDownload && targetDeno.exists() && denoVersion(targetDeno.absolutePath) != null) {
            return@withContext targetDeno.absolutePath
        }

        denoDir.mkdirs()

        try {
            val archive = File(denoDir, assetName)

            downloadFile(downloadUrl, archive, onProgress)

            // Extract zip archive
            NetAndArchive.extractZip(archive, denoDir)

            // Deno zip contains just the deno binary at the root
            val foundDeno = denoDir.listFiles()?.firstOrNull {
                it.isFile && it.name.matches(Regex("^deno(\\.exe)?$")) && it.canRead()
            } ?: error("Deno binary not found after extraction")

            if (foundDeno.absolutePath != targetDeno.absolutePath) {
                foundDeno.copyTo(targetDeno, overwrite = true)
            }

            if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                makeExecutable(targetDeno)
            }

            // Clean up archive
            archive.delete()

            denoVersion(targetDeno.absolutePath) ?: error("Deno is not runnable after installation")
            targetDeno.absolutePath
        } catch (t: Throwable) {
            errorln(t) { "Failed to download/install Deno: ${t.message}" }
            null
        }
    }

    // --- FFmpeg ---

    fun getDefaultFfmpegPath(): String {
        val dir = File(getDataDir(), "ffmpeg/bin")
        val exe = if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg"
        return File(dir, exe).absolutePath
    }

    suspend fun findFfmpegInSystemPath(): String? = withContext(Dispatchers.IO) {
        val cmd = if (getOperatingSystem() == OperatingSystem.WINDOWS) listOf("where", "ffmpeg") else listOf("which", "ffmpeg")
        try {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exited = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(1500) { p.waitFor() } }
            if (exited == null) {
                p.destroyForcibly(); return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull()?.trim() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun ffmpegVersion(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val p = ProcessBuilder(listOf(path, "-version")).redirectErrorStream(true).start()
            val exited = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(2_000) { p.waitFor() } }
            if (exited == null) {
                p.destroyForcibly(); return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun ffprobeVersion(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val p = ProcessBuilder(listOf(path, "-version")).redirectErrorStream(true).start()
            val exited = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(2_000) { p.waitFor() } }
            if (exited == null) {
                p.destroyForcibly(); return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull() else null
        } catch (_: Exception) {
            null
        }
    }

    fun getFfmpegAndFfprobeMacosAssetNamesForSystem(): Pair<String, String>? {
        if (getOperatingSystem() != OperatingSystem.MACOS) return null
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val suffix = if (arch.contains("aarch64") || arch.contains("arm64")) "darwin-arm64" else "darwin-x64"
        return "ffmpeg-$suffix" to "ffprobe-$suffix"
    }


    /**
     * Select the appropriate FFmpeg asset pattern for the current system.
     * Returns a pattern to match against available assets in the GitHub release.
     * Windows/Linux use yt-dlp FFmpeg-Builds archives.
     */
    fun getFfmpegAssetPatternForSystem(): String? {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64")
        return when (os) {
            OperatingSystem.WINDOWS -> when {
                isArm64 -> "winarm64-gpl.zip"
                arch.contains("32") || (arch.contains("x86") && !arch.contains("64")) -> "win32-gpl.zip"
                else -> "win64-gpl.zip"
            }
            OperatingSystem.LINUX -> if (isArm64) "linuxarm64-gpl.tar.xz" else "linux64-gpl.tar.xz"
            OperatingSystem.MACOS -> null
            else -> null
        }
    }

    /**
     * Download and install FFmpeg in the app cache, verifying it runs.
     * All platforms use archives (tar.xz or zip) that are extracted.
     *
     * @param archiveName The file name of the archive to download (e.g. "ffmpeg-master-latest-win64-gpl.zip").
     * @param forceDownload Re-download even if a working binary already exists.
     * @param downloadUrl Direct download URL for the archive.
     * @param onProgress Optional progress callback.
     */
    suspend fun downloadAndInstallFfmpeg(
        archiveName: String,
        forceDownload: Boolean,
        downloadUrl: String,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String? = ffmpegInstallMutex.withLock {
        withContext(Dispatchers.IO) {
            val baseDir = File(getDataDir(), "ffmpeg")
            val binDir = File(baseDir, "bin")
            val targetFfmpeg = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg")
            val targetFfprobe = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffprobe.exe" else "ffprobe")

            if (!forceDownload &&
                targetFfmpeg.exists() &&
                targetFfprobe.exists() &&
                ffmpegVersion(targetFfmpeg.absolutePath) != null
            ) {
                return@withContext targetFfmpeg.absolutePath
            }

            baseDir.mkdirs(); binDir.mkdirs()

            var extractDir: File? = null
            try {
                val archive = File(baseDir, archiveName)
                downloadFile(downloadUrl, archive, onProgress)

                // Extract in an isolated temp folder to avoid mixing stale files.
                extractDir = Files.createTempDirectory(baseDir.toPath(), "extract-").toFile()
                if (archive.name.endsWith(".zip")) NetAndArchive.extractZip(archive, extractDir)
                else if (archive.name.endsWith(".tar.xz")) NetAndArchive.extractTarXzWithSystemTar(archive, extractDir)
                else error("Unsupported FFmpeg archive: ${archive.name}")

                // Locate binaries only inside this extraction.
                val foundFfmpeg = extractDir.walkTopDown()
                    .firstOrNull { it.isFile && it.name.matches(Regex("^ffmpeg(\\.exe)?$")) && it.canRead() }
                    ?: error("FFmpeg binary not found after extraction")
                val foundFfprobe = extractDir.walkTopDown()
                    .firstOrNull { it.isFile && it.name.matches(Regex("^ffprobe(\\.exe)?$")) && it.canRead() }
                    ?: error("FFprobe binary not found after extraction")

                foundFfmpeg.copyTo(targetFfmpeg, overwrite = true)
                foundFfprobe.copyTo(targetFfprobe, overwrite = true)

                if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                    makeExecutable(targetFfmpeg)
                    makeExecutable(targetFfprobe)
                }

                ffmpegVersion(targetFfmpeg.absolutePath) ?: error("FFmpeg is not runnable after installation")
                if (!targetFfprobe.exists()) error("FFprobe is not available after installation")
                targetFfmpeg.absolutePath
            } catch (t: Throwable) {
                errorln(t) { "Failed to download/install FFmpeg: ${t.message}" }
                null
            } finally {
                extractDir?.deleteRecursively()
            }
        }
    }

    suspend fun downloadAndInstallFfmpegMacosBinaries(
        ffmpegAssetName: String,
        ffprobeAssetName: String,
        forceDownload: Boolean,
        ffmpegDownloadUrl: String,
        ffprobeDownloadUrl: String,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String? = ffmpegInstallMutex.withLock {
        withContext(Dispatchers.IO) {
            val baseDir = File(getDataDir(), "ffmpeg")
            val binDir = File(baseDir, "bin")
            val targetFfmpeg = File(binDir, "ffmpeg")
            val targetFfprobe = File(binDir, "ffprobe")

            if (!forceDownload &&
                targetFfmpeg.exists() &&
                targetFfprobe.exists() &&
                ffmpegVersion(targetFfmpeg.absolutePath) != null &&
                ffprobeVersion(targetFfprobe.absolutePath) != null
            ) {
                return@withContext targetFfmpeg.absolutePath
            }

            baseDir.mkdirs(); binDir.mkdirs()

            val ffmpegTemp = File(baseDir, "$ffmpegAssetName.tmp")
            val ffprobeTemp = File(baseDir, "$ffprobeAssetName.tmp")

            try {
                downloadFile(ffmpegDownloadUrl, ffmpegTemp, onProgress)
                downloadFile(ffprobeDownloadUrl, ffprobeTemp, null)

                ffmpegTemp.copyTo(targetFfmpeg, overwrite = true)
                ffprobeTemp.copyTo(targetFfprobe, overwrite = true)

                makeExecutable(targetFfmpeg)
                makeExecutable(targetFfprobe)

                ffmpegVersion(targetFfmpeg.absolutePath) ?: error("FFmpeg is not runnable after installation")
                ffprobeVersion(targetFfprobe.absolutePath) ?: error("FFprobe is not runnable after installation")
                targetFfmpeg.absolutePath
            } catch (t: Throwable) {
                errorln(t) { "Failed to download/install macOS FFmpeg binaries: ${t.message}" }
                null
            } finally {
                ffmpegTemp.delete()
                ffprobeTemp.delete()
            }
        }
    }
}
