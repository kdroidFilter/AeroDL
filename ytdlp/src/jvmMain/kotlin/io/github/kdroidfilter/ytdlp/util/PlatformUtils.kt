package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getCacheDir
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.ytdlp.core.platform.TrustedRootsSSL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import javax.net.ssl.HttpsURLConnection

object PlatformUtils {

    // --- yt-dlp ---

    fun getDefaultBinaryPath(): String {
        val dir = getCacheDir()
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
            val p = Runtime.getRuntime().exec(arrayOf("ldd", "--version"))
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
        val conn = (uri.toURL().openConnection() as HttpURLConnection).apply {
            if (this is HttpsURLConnection) {
                sslSocketFactory = TrustedRootsSSL.socketFactory
            }
            connectTimeout = 12_000
            readTimeout = 24_000
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
            }

        } catch (_: UnsupportedOperationException) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
        }
    }

    // --- FFmpeg ---

    fun getDefaultFfmpegPath(): String {
        val dir = File(getCacheDir(), "ffmpeg/bin")
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


    /**
     * Select the appropriate FFmpeg asset for the current system.
     * macOS: keep separate binaries for x64/arm64 as before (download URLs unchanged).
     */
    fun getFfmpegAssetNameForSystem(): String? {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64")
        return when (os) {
            OperatingSystem.WINDOWS -> when {
                isArm64 -> "ffmpeg-master-latest-winarm64-gpl.zip"
                arch.contains("32") || (arch.contains("x86") && !arch.contains("64")) -> "ffmpeg-master-latest-win32-gpl.zip"
                else -> "ffmpeg-master-latest-win64-gpl.zip"
            }
            OperatingSystem.LINUX -> if (isArm64) "ffmpeg-master-latest-linuxarm64-gpl.tar.xz"
            else "ffmpeg-master-latest-linux64-gpl.tar.xz"
            OperatingSystem.MACOS -> if (isArm64) "ffmpeg-darwin-arm64" else "ffmpeg-darwin-x64"
            else -> null
        }
    }

    /**
     * Download and install FFmpeg in the app cache, verifying it runs.
     * On macOS the asset is a plain binary; on Windows/Linux, archives are extracted.
     */
    suspend fun downloadAndInstallFfmpeg(
        assetName: String,
        forceDownload: Boolean,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        val baseDir = File(getCacheDir(), "ffmpeg")
        val binDir = File(baseDir, "bin")
        val targetExe = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg")

        if (targetExe.exists() && ffmpegVersion(targetExe.absolutePath) != null && !forceDownload) {
            return@withContext targetExe.absolutePath
        }

        baseDir.mkdirs(); binDir.mkdirs()

        val os = getOperatingSystem()
        val archive = File(baseDir, assetName)
        val url = when (os) {
            OperatingSystem.MACOS -> "https://github.com/eugeneware/ffmpeg-static/releases/download/b6.0/$assetName"
            OperatingSystem.WINDOWS, OperatingSystem.LINUX -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/latest/download/$assetName"
            else -> "https://github.com/yt-dlp/FFmpeg-Builds/releases/latest/download/$assetName"
        }

        try {
            downloadFile(url, archive, onProgress)

            if (os == OperatingSystem.MACOS) {
                // macOS assets are plain binaries (no archive)
                archive.copyTo(targetExe, overwrite = true)
            } else {
                if (assetName.endsWith(".zip")) NetAndArchive.extractZip(archive, baseDir)
                else if (assetName.endsWith(".tar.xz")) NetAndArchive.extractTarXzWithSystemTar(archive, baseDir)
                else error("Unsupported FFmpeg archive: $assetName")

                val found = baseDir.walkTopDown()
                    .firstOrNull { it.isFile && it.name.startsWith("ffmpeg") && it.canRead() }
                    ?: error("FFmpeg binary not found after extraction")

                found.copyTo(targetExe, overwrite = true)
            }

            if (getOperatingSystem() != OperatingSystem.WINDOWS) makeExecutable(targetExe)

            ffmpegVersion(targetExe.absolutePath) ?: error("FFmpeg is not runnable after installation")
            targetExe.absolutePath
        } catch (t: Throwable) {
            errorln { "Failed to download/install FFmpeg: ${t.stackTraceToString()}" }
            null
        }
    }
}