package io.github.kdroidfilter.ytdlp.util

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getCacheDir
import io.github.kdroidfilter.platformtools.getOperatingSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

object PlatformUtils {

    // ---- yt-dlp ----
    fun getDefaultBinaryPath(): String {
        val dir = getCacheDir()
        val os = getOperatingSystem()
        val binaryName = when (os) {
            OperatingSystem.WINDOWS -> "yt-dlp.exe"
            else -> "yt-dlp"
        }
        return File(dir, binaryName).absolutePath
    }

    fun getYtDlpAssetNameForSystem(): String {
        val os = getOperatingSystem()
        val arch = (System.getProperty("os.arch") ?: "").lowercase()
        return when (os) {
            OperatingSystem.WINDOWS -> when {
                arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_win_arm64.exe"
                arch.contains("32") || (arch.contains("x86") && !arch.contains("64")) -> "yt-dlp_x86.exe"
                else -> "yt-dlp.exe"
            }
            OperatingSystem.MACOS -> when {
                arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_macos_arm64"
                else -> "yt-dlp_macos"
            }
            OperatingSystem.LINUX -> {
                val isMusl = try {
                    val p = Runtime.getRuntime().exec(arrayOf("ldd", "--version"))
                    val out = p.inputStream.bufferedReader().readText()
                    out.contains("musl")
                } catch (_: Exception) { false }

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

    fun downloadFile(url: String, dest: File) {
        dest.parentFile?.mkdirs()
        java.net.URI.create(url).toURL().openStream().use { input ->
            Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun makeExecutable(file: File) {
        try {
            val path = file.toPath()
            val perms = Files.getPosixFilePermissions(path).toMutableSet()
            perms.add(PosixFilePermission.OWNER_EXECUTE)
            perms.add(PosixFilePermission.GROUP_EXECUTE)
            perms.add(PosixFilePermission.OTHERS_EXECUTE)
            Files.setPosixFilePermissions(path, perms)
        } catch (_: UnsupportedOperationException) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
        }
    }

    // ---- FFmpeg ----
    fun getDefaultFfmpegPath(): String {
        val dir = File(getCacheDir(), "ffmpeg/bin")
        val exe = if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg"
        return File(dir, exe).absolutePath
    }

    fun findFfmpegInSystemPath(): String? {
        val cmd = if (getOperatingSystem() == OperatingSystem.WINDOWS) listOf("where", "ffmpeg") else listOf("which", "ffmpeg")
        return try {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText().trim()
            if (p.waitFor() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull()?.trim() else null
        } catch (_: Exception) { null }
    }

    fun ffmpegVersion(path: String): String? = try {
        val p = ProcessBuilder(listOf(path, "-version")).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText().trim()
        if (p.waitFor() == 0 && out.isNotBlank()) out.lineSequence().firstOrNull() else null
    } catch (_: Exception) { null }

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
            OperatingSystem.MACOS -> null
            else -> null
        }
    }

    /**
     * Download & install FFmpeg. Returns installed binary path or null on failure.
     */
    suspend fun downloadAndInstallFfmpeg(assetName: String, forceDownload: Boolean): String? {
        val baseDir = File(getCacheDir(), "ffmpeg")
        val binDir = File(baseDir, "bin")
        val targetExe = File(binDir, if (getOperatingSystem() == OperatingSystem.WINDOWS) "ffmpeg.exe" else "ffmpeg")

        if (targetExe.exists() && ffmpegVersion(targetExe.absolutePath) != null && !forceDownload) {
            return targetExe.absolutePath
        }

        baseDir.mkdirs(); binDir.mkdirs()
        val url = "https://github.com/yt-dlp/FFmpeg-Builds/releases/latest/download/$assetName"
        val archive = File(baseDir, assetName)

        return try {
            java.net.URI.create(url).toURL().openStream().use { ins ->
                Files.copy(ins, archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            if (assetName.endsWith(".zip")) NetAndArchive.extractZip(archive, baseDir)
            else if (assetName.endsWith(".tar.xz")) NetAndArchive.extractTarXzWithSystemTar(archive, baseDir)
            else error("Unsupported FFmpeg archive: $assetName")

            val found = baseDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.startsWith("ffmpeg") && it.canRead() }
                ?: error("FFmpeg binary not found after extraction")

            found.copyTo(targetExe, overwrite = true)

            if (getOperatingSystem() != OperatingSystem.WINDOWS) makeExecutable(targetExe)

            ffmpegVersion(targetExe.absolutePath) ?: error("FFmpeg not runnable")
            targetExe.absolutePath
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
