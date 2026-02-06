package io.github.kdroidfilter.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Post-processes a .deb package to:
 * - Inject a .desktop file (required when using jpackage --app-image mode)
 * - Fix t64 dependencies for Ubuntu 24.04 compatibility
 */
@DisableCachingByDefault(because = "Modifies .deb in place")
abstract class DebPostProcessTask : DefaultTask() {

    @get:InputDirectory
    abstract val debDirectory: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val execPath: Property<String>

    @get:Input
    abstract val iconPath: Property<String>

    @get:Input
    @get:Optional
    abstract val startupWMClass: Property<String>

    @get:Input
    @get:Optional
    abstract val categories: Property<String>

    @get:Input
    abstract val enableT64AlternativeDeps: Property<Boolean>

    init {
        group = "distribution"
        description = "Post-process .deb: inject .desktop file and fix dependencies"

        // Set defaults
        categories.convention("AudioVideo;Audio;Video;Network;")
        enableT64AlternativeDeps.convention(true)
    }

    @TaskAction
    fun execute() {
        if (!System.getProperty("os.name").lowercase().contains("linux")) {
            logger.lifecycle("[debPostProcess] Skipped (non-Linux OS)")
            return
        }

        val debDir = debDirectory.get().asFile
        val debFile = debDir.listFiles()?.firstOrNull { it.extension == "deb" }
        if (debFile == null) {
            logger.warn("[debPostProcess] No .deb file found in ${debDir.absolutePath}")
            return
        }

        logger.lifecycle("[debPostProcess] Processing ${debFile.name}")

        val workDir = File(debFile.parentFile, "deb-work")
        try {
            processDebFile(debFile, workDir)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun processDebFile(debFile: File, workDir: File) {
        val extractDir = File(workDir, "extract")
        val dataDir = File(workDir, "data")
        val controlDir = File(workDir, "control")

        // Clean and create work directories
        workDir.deleteRecursively()
        listOf(extractDir, dataDir, controlDir).forEach { it.mkdirs() }

        // Extract .deb archive
        exec("ar", "x", debFile.absolutePath, workDir = extractDir)

        // Find data.tar.*
        val dataTar = extractDir.listFiles()?.firstOrNull { it.name.startsWith("data.tar") }
            ?: error("data.tar.* not found in .deb")

        // Extract data.tar
        extractTar(dataTar, dataDir)

        // Create .desktop file
        createDesktopFile(dataDir)

        // Recompress data.tar
        val newDataTar = File(extractDir, dataTar.name)
        dataTar.delete()
        compressTar(newDataTar, dataDir)

        // Process control.tar if t64 deps enabled
        val controlTar = extractDir.listFiles()?.firstOrNull { it.name.startsWith("control.tar") }
        if (controlTar != null && enableT64AlternativeDeps.get()) {
            extractTar(controlTar, controlDir)
            fixT64Dependencies(controlDir)
            val newControlTar = File(extractDir, controlTar.name)
            controlTar.delete()
            compressTar(newControlTar, controlDir)
        }

        // Rebuild .deb
        rebuildDeb(debFile, extractDir)

        logger.lifecycle("[debPostProcess] Successfully processed ${debFile.name}")
    }

    private fun createDesktopFile(dataDir: File) {
        val appsDir = File(dataDir, "usr/share/applications")
        appsDir.mkdirs()

        val desktopFileName = "${packageName.get()}-${appName.get()}.desktop"
        val desktopFile = File(appsDir, desktopFileName)

        val content = buildString {
            appendLine("[Desktop Entry]")
            appendLine("Name=${appName.get()}")
            appendLine("Comment=${appName.get()}")
            appendLine("Exec=${execPath.get()} %U")
            appendLine("Icon=${iconPath.get()}")
            appendLine("Terminal=false")
            appendLine("Type=Application")
            appendLine("Categories=${categories.get()}")
            if (startupWMClass.isPresent) {
                appendLine("StartupWMClass=${startupWMClass.get()}")
            }
        }

        desktopFile.writeText(content)
        logger.lifecycle("[debPostProcess] Created ${desktopFile.name}")
    }

    private fun fixT64Dependencies(controlDir: File) {
        val controlFile = File(controlDir, "control")
        if (!controlFile.exists()) return

        var content = controlFile.readText()
        val t64Mappings = mapOf(
            "libc6" to "libc6 | libc6t64",
            "libgtk-3-0" to "libgtk-3-0 | libgtk-3-0t64",
            "libglib2.0-0" to "libglib2.0-0 | libglib2.0-0t64",
            "libx11-6" to "libx11-6 | libx11-6t64"
        )

        var modified = false
        for ((old, new) in t64Mappings) {
            if (content.contains(old) && !content.contains("$old |") && !content.contains("| ${old}t64")) {
                content = content.replace(Regex("\\b${Regex.escape(old)}\\b(?!t64)"), new)
                modified = true
            }
        }

        if (modified) {
            controlFile.writeText(content)
            logger.lifecycle("[debPostProcess] Updated control file with t64 alternatives")
        }
    }

    private fun extractTar(tarFile: File, destDir: File) {
        val args = when {
            tarFile.name.endsWith(".zst") -> listOf("tar", "--zstd", "-xf", tarFile.absolutePath, "-C", destDir.absolutePath)
            tarFile.name.endsWith(".xz") -> listOf("tar", "-xJf", tarFile.absolutePath, "-C", destDir.absolutePath)
            tarFile.name.endsWith(".gz") -> listOf("tar", "-xzf", tarFile.absolutePath, "-C", destDir.absolutePath)
            else -> listOf("tar", "-xf", tarFile.absolutePath, "-C", destDir.absolutePath)
        }
        exec(*args.toTypedArray())
    }

    private fun compressTar(tarFile: File, sourceDir: File) {
        val cmd = when {
            tarFile.name.endsWith(".zst") -> "tar -cf - -C ${sourceDir.absolutePath} . | zstd -19 -o ${tarFile.absolutePath}"
            tarFile.name.endsWith(".xz") -> "tar -cJf ${tarFile.absolutePath} -C ${sourceDir.absolutePath} ."
            tarFile.name.endsWith(".gz") -> "tar -czf ${tarFile.absolutePath} -C ${sourceDir.absolutePath} ."
            else -> "tar -cf ${tarFile.absolutePath} -C ${sourceDir.absolutePath} ."
        }
        exec("sh", "-c", cmd)
    }

    private fun rebuildDeb(debFile: File, extractDir: File) {
        val debianBinary = File(extractDir, "debian-binary")
        val controlTar = extractDir.listFiles()?.firstOrNull { it.name.startsWith("control.tar") }
        val dataTar = extractDir.listFiles()?.firstOrNull { it.name.startsWith("data.tar") }

        if (debianBinary.exists() && controlTar != null && dataTar != null) {
            debFile.delete()
            exec("ar", "rcs", debFile.absolutePath,
                debianBinary.absolutePath, controlTar.absolutePath, dataTar.absolutePath)
        } else {
            error("Missing components to rebuild .deb")
        }
    }

    private fun exec(vararg args: String, workDir: File? = null) {
        val process = ProcessBuilder(*args)
            .apply { workDir?.let { directory(it) } }
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            error("Command failed with exit code $exitCode: ${args.joinToString(" ")}\n$output")
        }
    }
}
