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
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

object PlatformUtils {

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
        val conn = HttpsConnectionFactory.openConnection(uri.toURL()) {
            connectTimeout = 12_000
            readTimeout = 24_000
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
            }

        } catch (_: UnsupportedOperationException) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
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


    /**
     * Select the appropriate FFmpeg asset pattern for the current system.
     * Returns a pattern to match against available assets in the GitHub release.
     * All platforms use kdroidFilter/FFmpeg-Builds repo with tar.xz/zip archives.
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
            OperatingSystem.MACOS -> if (isArm64) "macosarm64-gpl.tar.xz" else "macos64-gpl.tar.xz"
            else -> null
        }
    }

    /**
     * Download and install FFmpeg in the app cache, verifying it runs.
     * All platforms use archives (tar.xz or zip) that are extracted.
     * Uses GitHubReleaseFetcher to get the direct download URL.
     * For macOS: kdroidFilter/FFmpeg-Builds; for Windows/Linux: yt-dlp/FFmpeg-Builds.
     */
    suspend fun downloadAndInstallFfmpeg(
        assetPattern: String,
        forceDownload: Boolean,
        ffmpegFetcher: io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        val baseDir = File(getDataDir(), "ffmpeg")
        val binDir = File(baseDir, "bin")
        val targetFfmpeg = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg")
        val targetFfprobe = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffprobe.exe" else "ffprobe")

        if (!forceDownload && targetFfmpeg.exists() && ffmpegVersion(targetFfmpeg.absolutePath) != null) {
            if (targetFfprobe.exists()) {
                return@withContext targetFfmpeg.absolutePath
            }
            // Continue to (re)install to retrieve ffprobe as well.
        }

        baseDir.mkdirs(); binDir.mkdirs()

        try {
            val release = ffmpegFetcher.getLatestRelease() ?: error("Could not fetch FFmpeg release from GitHub")

            val asset = release.assets.find { it.name.endsWith(assetPattern) && !it.name.contains("shared") }
                ?: error("Asset matching pattern '$assetPattern' not found in FFmpeg release. Available assets: ${release.assets.map { it.name }}")

            val url = asset.browser_download_url
            val archive = File(baseDir, asset.name)

            downloadFile(url, archive, onProgress)

            // Extract archive
            if (archive.name.endsWith(".zip")) NetAndArchive.extractZip(archive, baseDir)
            else if (archive.name.endsWith(".tar.xz")) NetAndArchive.extractTarXzWithSystemTar(archive, baseDir)
            else error("Unsupported FFmpeg archive: ${archive.name}")

            // Locate ffmpeg and ffprobe within the extracted tree
            val foundFfmpeg = baseDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.matches(Regex("^ffmpeg(\\.exe)?$")) && it.canRead() }
                ?: error("FFmpeg binary not found after extraction")
            foundFfmpeg.copyTo(targetFfmpeg, overwrite = true)

            val foundFfprobe = baseDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.matches(Regex("^ffprobe(\\.exe)?$")) && it.canRead() }
            if (foundFfprobe != null) {
                foundFfprobe.copyTo(targetFfprobe, overwrite = true)
            }

            if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                makeExecutable(targetFfmpeg)
                if (targetFfprobe.exists()) makeExecutable(targetFfprobe)
            }

            ffmpegVersion(targetFfmpeg.absolutePath) ?: error("FFmpeg is not runnable after installation")
            targetFfmpeg.absolutePath
        } catch (t: Throwable) {
            errorln { "Failed to download/install FFmpeg: ${t.stackTraceToString()}" }
            null
        }
    }
}
