package io.github.kdroidfilter.ffmpeg.util

import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.network.HttpsConnectionFactory
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/**
 * Platform utilities for FFmpeg binary management.
 */
object PlatformUtils {

    private fun getDataDir(): String = FileKit.databasesDir.path

    // --- FFmpeg Paths ---

    fun getDefaultFfmpegPath(): String {
        val dir = File(getDataDir(), "ffmpeg/bin")
        val exe = if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg"
        return File(dir, exe).absolutePath
    }

    fun getDefaultFfprobePath(): String {
        val dir = File(getDataDir(), "ffmpeg/bin")
        val exe = if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffprobe.exe" else "ffprobe"
        return File(dir, exe).absolutePath
    }

    // --- System PATH Detection ---

    suspend fun findFfmpegInSystemPath(): String? = withContext(Dispatchers.IO) {
        val cmd = if (getOperatingSystem() == OperatingSystem.WINDOWS)
            listOf("where", "ffmpeg")
        else
            listOf("which", "ffmpeg")

        runCatching {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exited = withTimeoutOrNull(1500) { p.waitFor() }
            if (exited == null) {
                p.destroyForcibly()
                return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull()?.trim() else null
        }.getOrNull()
    }

    suspend fun findFfprobeInSystemPath(): String? = withContext(Dispatchers.IO) {
        val cmd = if (getOperatingSystem() == OperatingSystem.WINDOWS)
            listOf("where", "ffprobe")
        else
            listOf("which", "ffprobe")

        runCatching {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exited = withTimeoutOrNull(1500) { p.waitFor() }
            if (exited == null) {
                p.destroyForcibly()
                return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull()?.trim() else null
        }.getOrNull()
    }

    // --- Version Detection ---

    suspend fun getFfmpegVersion(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val p = ProcessBuilder(listOf(path, "-version")).redirectErrorStream(true).start()
            val exited = withTimeoutOrNull(2_000) { p.waitFor() }
            if (exited == null) {
                p.destroyForcibly()
                return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) {
                // Parse "ffmpeg version X.Y.Z ..."
                out.lineSequence().firstOrNull()?.let { line ->
                    Regex("""ffmpeg version (\S+)""").find(line)?.groupValues?.get(1) ?: line
                }
            } else null
        }.getOrNull()
    }

    suspend fun getFfprobeVersion(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val p = ProcessBuilder(listOf(path, "-version")).redirectErrorStream(true).start()
            val exited = withTimeoutOrNull(2_000) { p.waitFor() }
            if (exited == null) {
                p.destroyForcibly()
                return@withContext null
            }
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.exitValue() == 0 && out.isNotBlank()) {
                out.lineSequence().firstOrNull()?.let { line ->
                    Regex("""ffprobe version (\S+)""").find(line)?.groupValues?.get(1) ?: line
                }
            } else null
        }.getOrNull()
    }

    // --- Asset Pattern Detection ---

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

    // --- Download & Installation ---

    /**
     * Download and install FFmpeg in the app cache, verifying it runs.
     *
     * @param archiveName The file name of the archive to download.
     * @param forceDownload Re-download even if a working binary already exists.
     * @param downloadUrl Direct download URL for the archive.
     * @param onProgress Optional progress callback.
     */
    suspend fun downloadAndInstallFfmpeg(
        archiveName: String,
        forceDownload: Boolean,
        downloadUrl: String,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        val baseDir = File(getDataDir(), "ffmpeg")
        val binDir = File(baseDir, "bin")
        val targetFfmpeg = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg")
        val targetFfprobe = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffprobe.exe" else "ffprobe")

        if (!forceDownload && targetFfmpeg.exists() && getFfmpegVersion(targetFfmpeg.absolutePath) != null) {
            if (targetFfprobe.exists()) {
                return@withContext targetFfmpeg.absolutePath
            }
        }

        baseDir.mkdirs()
        binDir.mkdirs()

        try {
            val archive = File(baseDir, archiveName)

            downloadFile(downloadUrl, archive, onProgress)

            // Extract archive
            if (archive.name.endsWith(".zip")) extractZip(archive, baseDir)
            else if (archive.name.endsWith(".tar.xz")) extractTarXz(archive, baseDir)
            else error("Unsupported FFmpeg archive: ${archive.name}")

            val foundFfmpeg = baseDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.matches(Regex("^ffmpeg(\\.exe)?$")) && it.canRead() }
                ?: error("FFmpeg binary not found after extraction")
            foundFfmpeg.copyTo(targetFfmpeg, overwrite = true)

            val foundFfprobe = baseDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.matches(Regex("^ffprobe(\\.exe)?$")) && it.canRead() }
            foundFfprobe?.copyTo(targetFfprobe, overwrite = true)

            if (getOperatingSystem() != OperatingSystem.WINDOWS) {
                makeExecutable(targetFfmpeg)
                if (targetFfprobe.exists()) makeExecutable(targetFfprobe)
            }

            getFfmpegVersion(targetFfmpeg.absolutePath) ?: error("FFmpeg is not runnable after installation")
            targetFfmpeg.absolutePath
        } catch (t: Throwable) {
            errorln { "Failed to download/install FFmpeg: ${t.stackTraceToString()}" }
            null
        }
    }

    // --- Helper Functions ---

    private suspend fun downloadFile(
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
            setRequestProperty("User-Agent", "kdroidFilter-ffmpeg/1.0")
        }

        val total = conn.getHeaderFieldLong("Content-Length", -1L).takeIf { it > 0 }
        conn.inputStream.use { input ->
            Channels.newChannel(input).use { rch ->
                java.io.FileOutputStream(dest).use { fos ->
                    val buffer = ByteBuffer.allocateDirect(1024 * 64)
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

    private suspend fun makeExecutable(file: File) = withContext(Dispatchers.IO) {
        try {
            val path = file.toPath()
            val perms = Files.getPosixFilePermissions(path).toMutableSet()
            perms.add(PosixFilePermission.OWNER_EXECUTE)
            perms.add(PosixFilePermission.GROUP_EXECUTE)
            perms.add(PosixFilePermission.OTHERS_EXECUTE)
            Files.setPosixFilePermissions(path, perms)

            if (getOperatingSystem() == OperatingSystem.MACOS) {
                runCatching {
                    ProcessBuilder("xattr", "-d", "com.apple.quarantine", file.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor()
                }
            }
        } catch (_: UnsupportedOperationException) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        java.util.zip.ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val target = File(destDir, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private suspend fun extractTarXz(archive: File, destDir: File) = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("tar", "-xf", archive.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true)
            .start()

        val exited = withTimeoutOrNull(60_000) { process.waitFor() }
        if (exited == null) {
            process.destroyForcibly()
            error("Tar extraction timed out")
        }
        if (process.exitValue() != 0) {
            error("Tar extraction failed: ${process.inputStream.bufferedReader().readText()}")
        }
    }
}
