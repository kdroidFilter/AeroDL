package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getCacheDir
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JVM wrapper around yt-dlp CLI, with configurable binary paths.
 */
class YtDlpWrapper(
    private val fetcher: GitHubReleaseFetcher
) {
    /** Configuration for paths */
    var ytDlpPath: String = getDefaultBinaryPath()
    var ffmpegPath: String? = null
    var downloadDir: File? = null

    companion object {
        /**
         * Obtient le chemin par défaut pour le binaire yt-dlp dans le cache
         */
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
         * Obtient le nom du fichier binaire approprié pour le système actuel
         */
        fun getAssetNameForSystem(): String {
            val os = getOperatingSystem()
            val arch = System.getProperty("os.arch")?.lowercase() ?: ""
            val isArm = arch.contains("arm") || arch.contains("aarch")
            val is64Bit = arch.contains("64")
            val isX86 = arch.contains("x86") && !is64Bit

            return when (os) {
                OperatingSystem.WINDOWS -> when {
                    isArm -> "yt-dlp_arm64.exe"
                    isX86 -> "yt-dlp_x86.exe"
                    else -> "yt-dlp.exe"  // x64 par défaut
                }

                OperatingSystem.MACOS -> "yt-dlp_macos"

                OperatingSystem.LINUX -> {
                    // Déterminer si c'est un système musl (Alpine, etc.)
                    val isMusl = try {
                        val process = Runtime.getRuntime().exec(arrayOf("ldd", "--version"))
                        val output = process.inputStream.bufferedReader().readText()
                        output.contains("musl")
                    } catch (e: Exception) {
                        false
                    }

                    when {
                        isMusl && arch.contains("aarch64") -> "yt-dlp_musllinux_aarch64"
                        isMusl -> "yt-dlp_musllinux"
                        arch.contains("aarch64") -> "yt-dlp_linux_aarch64"
                        arch.contains("armv7") -> "yt-dlp_linux_armv7l"
                        else -> "yt-dlp_linux"
                    }
                }

                else -> "yt-dlp"  // Fallback
            }
        }
    }

    data class Options(
        val format: String? = null,            // e.g. "bestvideo+bestaudio/best"
        val outputTemplate: String? = null,    // e.g. "%(title)s.%(ext)s"
        val noCheckCertificate: Boolean = false,
        val extraArgs: List<String> = emptyList()
    )

    sealed class Event {
        data class Progress(val percent: Double?, val rawLine: String) : Event()
        data class Log(val line: String) : Event()
        data class Completed(val exitCode: Int) : Event()
        data class Error(val message: String, val cause: Throwable? = null) : Event()
        data object Started : Event()
        data object Cancelled : Event()
    }

    class Handle internal constructor(private val process: Process, private val cancelledFlag: AtomicBoolean) {
        fun cancel() {
            cancelledFlag.set(true)
            process.destroy()
        }
        val isCancelled: Boolean get() = cancelledFlag.get()
    }

    init {
        // Vérifier d'abord si le binaire existe déjà dans le cache
        val binaryFile = File(ytDlpPath)
        if (!binaryFile.exists()) {
            // Si le binaire n'existe pas dans le cache, essayer de le trouver dans le PATH système
            val systemPath = findInSystemPath()
            if (systemPath != null) {
                ytDlpPath = systemPath
                println("yt-dlp found in system PATH: $ytDlpPath")
            } else {
                println("yt-dlp not found at path: $ytDlpPath")
                println("Call downloadBinary() to download it automatically")
            }
        } else {
            // Le binaire existe dans le cache
            println("Using cached yt-dlp binary: $ytDlpPath")
        }
    }

    /**
     * Cherche yt-dlp dans le PATH système
     */
    private fun findInSystemPath(): String? {
        val os = getOperatingSystem()
        val command = when (os) {
            OperatingSystem.WINDOWS -> listOf("where", "yt-dlp")
            else -> listOf("which", "yt-dlp")
        }

        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.isNotBlank()) {
                output.lines().firstOrNull()?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /** Returns yt-dlp version or null if unavailable */
    fun version(): String? = try {
        val proc = ProcessBuilder(listOf(ytDlpPath, "--version"))
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val code = proc.waitFor()
        if (code == 0 && out.isNotBlank()) out else null
    } catch (_: Exception) {
        null
    }

    fun isAvailable(): Boolean {
        // Vérifier si le fichier existe et est exécutable
        val file = File(ytDlpPath)
        return file.exists() && file.canExecute() && version() != null
    }

    /**
     * Check if an update is available
     * @return true if current version is different from latest release
     */
    suspend fun hasUpdate(): Boolean {
        val currentVersion = version() ?: return true  // Si pas de version, mise à jour nécessaire
        val latestVersion = fetcher.getLatestRelease()?.tag_name ?: return false

        // Nettoyer les versions (enlever 'v' prefix si présent)
        val current = currentVersion.removePrefix("v").trim()
        val latest = latestVersion.removePrefix("v").trim()

        return current != latest
    }

    /**
     * Check if binary needs to be downloaded (doesn't exist or is corrupt)
     */
    fun needsDownload(): Boolean {
        val file = File(ytDlpPath)

        // Si le fichier n'existe pas
        if (!file.exists()) {
            return true
        }

        // Si le fichier existe mais n'est pas exécutable
        if (!file.canExecute()) {
            return true
        }

        // Si le fichier existe mais ne répond pas correctement
        return version() == null
    }

    /**
     * Downloads the appropriate yt-dlp binary for the current OS and architecture.
     * @param forceDownload Force download even if binary already exists
     * @return true if download successful, false otherwise
     */
    suspend fun downloadBinary(forceDownload: Boolean = false): Boolean {
        return try {
            val destFile = File(ytDlpPath)

            // Si le binaire existe déjà et qu'on ne force pas le téléchargement
            if (destFile.exists() && !forceDownload) {
                println("Binary already exists at: ${destFile.absolutePath}")

                // Vérifier que le binaire fonctionne
                if (isAvailable()) {
                    println("Binary is functional, skipping download")
                    return true
                } else {
                    println("Binary exists but is not functional, re-downloading...")
                }
            }

            // Créer le répertoire si nécessaire
            destFile.parentFile?.mkdirs()

            val os = getOperatingSystem()
            val assetName = getAssetNameForSystem()

            // Obtenir la dernière release
            val release = fetcher.getLatestRelease() ?: return false
            val assets = release.assets

            // Trouver l'asset correspondant
            val asset = assets.find { it.name == assetName } ?: return false

            println("Downloading $assetName from ${release.tag_name}...")

            // Télécharger le fichier
            downloadFile(asset.browser_download_url, destFile)

            // Rendre exécutable sur Unix-like systems
            if (os != OperatingSystem.WINDOWS) {
                makeExecutable(destFile)
            }

            // Vérifier que le téléchargement a réussi
            if (isAvailable()) {
                println("Successfully downloaded yt-dlp to: ${destFile.absolutePath}")
                true
            } else {
                println("Download completed but binary is not functional")
                destFile.delete()
                false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Download a file from URL to destination
     */
    private fun downloadFile(url: String, destFile: File) {
        java.net.URI.create(url).toURL().openStream().use { input ->
            Files.copy(input, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Make file executable on Unix-like systems
     */
    private fun makeExecutable(file: File) {
        try {
            // Méthode 1 : Utiliser les permissions POSIX si disponibles
            val path = file.toPath()
            val perms = Files.getPosixFilePermissions(path)
            perms.add(PosixFilePermission.OWNER_EXECUTE)
            perms.add(PosixFilePermission.GROUP_EXECUTE)
            perms.add(PosixFilePermission.OTHERS_EXECUTE)
            Files.setPosixFilePermissions(path, perms)
        } catch (e: UnsupportedOperationException) {
            // Méthode 2 : Fallback avec Runtime.exec pour les systèmes non-POSIX
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
            } catch (ignored: Exception) {
                // Si chmod n'existe pas, on ignore (peut-être sur Windows WSL)
            }
        }
    }

    /**
     * Run yt-dlp with the given url and options.
     * Blocking until finished/cancelled. Run from a background thread.
     */
    fun download(
        url: String,
        options: Options = Options(),
        onEvent: (Event) -> Unit = {}
    ): Handle {
        val cmd = buildCommand(url, options)
        val pb = ProcessBuilder(cmd)
            .redirectErrorStream(true)

        // configure working directory if provided
        downloadDir?.let { pb.directory(it) }

        val process = pb.start()
        val cancelled = AtomicBoolean(false)
        onEvent(Event.Started)

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!
                val progress = parseProgress(l)
                if (progress != null) onEvent(progress) else onEvent(Event.Log(l))
            }
        } catch (t: Throwable) {
            if (!cancelled.get()) onEvent(Event.Error("I/O error while reading yt-dlp output", t))
        } finally {
            reader.close()
        }

        val exit = try { process.waitFor() } catch (e: InterruptedException) { -1 }
        if (cancelled.get()) onEvent(Event.Cancelled) else onEvent(Event.Completed(exit))
        return Handle(process, cancelled)
    }

    private fun buildCommand(url: String, options: Options): List<String> {
        val cmd = mutableListOf(ytDlpPath, "--newline")

        // ffmpeg override
        if (!ffmpegPath.isNullOrBlank()) {
            cmd.addAll(listOf("--ffmpeg-location", ffmpegPath!!))
        }

        // no-check-certificate
        if (options.noCheckCertificate) {
            cmd.add("--no-check-certificate")
        }

        if (!options.format.isNullOrBlank()) {
            cmd.addAll(listOf("-f", options.format))
        }
        if (!options.outputTemplate.isNullOrBlank()) {
            cmd.addAll(listOf("-o", options.outputTemplate))
        }
        if (options.extraArgs.isNotEmpty()) {
            cmd.addAll(options.extraArgs)
        }
        cmd.add(url)
        return cmd
    }

    private fun parseProgress(line: String): Event.Progress? {
        val percentRegex = Regex("(\\d{1,3}(?:[.,]\\d+)?)%")
        val match = percentRegex.find(line)
        val pct = match?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        return if (pct != null) Event.Progress(pct, line) else null
    }
}