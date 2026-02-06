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
 * Post-processes a .rpm package to inject a .desktop file.
 * Required when using jpackage --app-image mode, which skips .desktop generation.
 *
 * Uses rpm2cpio/cpio to extract, adds the .desktop file, then rebuilds with rpmbuild.
 */
@DisableCachingByDefault(because = "Modifies .rpm in place")
abstract class RpmPostProcessTask : DefaultTask() {

    @get:InputDirectory
    abstract val rpmDirectory: DirectoryProperty

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

    init {
        group = "distribution"
        description = "Post-process .rpm: inject .desktop file"
        categories.convention("AudioVideo;Audio;Video;Network;")
    }

    @TaskAction
    fun execute() {
        if (!System.getProperty("os.name").lowercase().contains("linux")) {
            logger.lifecycle("[rpmPostProcess] Skipped (non-Linux OS)")
            return
        }

        val rpmDir = rpmDirectory.get().asFile
        val rpmFile = rpmDir.listFiles()?.firstOrNull { it.extension == "rpm" }
        if (rpmFile == null) {
            logger.warn("[rpmPostProcess] No .rpm file found in ${rpmDir.absolutePath}")
            return
        }

        logger.lifecycle("[rpmPostProcess] Processing ${rpmFile.name}")

        val workDir = File(rpmFile.parentFile, "rpm-work")
        try {
            processRpmFile(rpmFile, workDir)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun processRpmFile(rpmFile: File, workDir: File) {
        workDir.deleteRecursively()
        workDir.mkdirs()

        // Create a local rpmdb to avoid permission issues with the system rpmdb
        val localRpmDb = File(workDir, "rpmdb")
        localRpmDb.mkdirs()

        // Extract RPM contents
        val extractDir = File(workDir, "root")
        extractDir.mkdirs()
        exec("sh", "-c", "cd '${extractDir.absolutePath}' && rpm2cpio '${rpmFile.absolutePath}' | cpio -idmv")

        // Create .desktop file
        createDesktopFile(extractDir)

        // Query RPM metadata (stderr separated to avoid rpmdb warnings polluting values)
        val name = queryRpm(rpmFile, "%{NAME}")
        val rpmVersion = queryRpm(rpmFile, "%{VERSION}")
        val release = queryRpm(rpmFile, "%{RELEASE}")
        val arch = queryRpm(rpmFile, "%{ARCH}")
        val summary = queryRpm(rpmFile, "%{SUMMARY}").ifEmpty { appName.get() }
        val license = queryRpm(rpmFile, "%{LICENSE}").ifEmpty { "Unknown" }
        val description = queryRpm(rpmFile, "%{DESCRIPTION}").ifEmpty { appName.get() }

        logger.lifecycle("[rpmPostProcess] RPM metadata: name=$name version=$rpmVersion release=$release arch=$arch")

        // Query pre/post scripts to preserve them
        val preIn = queryRpm(rpmFile, "%{PREIN}")
        val postIn = queryRpm(rpmFile, "%{POSTIN}")
        val preUn = queryRpm(rpmFile, "%{PREUN}")
        val postUn = queryRpm(rpmFile, "%{POSTUN}")

        // Build file list from extracted tree
        val fileEntries = buildString {
            extractDir.walkTopDown().sorted().forEach { file ->
                val relPath = "/" + file.relativeTo(extractDir).path
                if (relPath == "/") return@forEach
                if (file.isDirectory) {
                    appendLine("%dir \"$relPath\"")
                } else {
                    appendLine("\"$relPath\"")
                }
            }
        }

        // Generate spec file
        val specContent = buildString {
            appendLine("Name: $name")
            appendLine("Version: $rpmVersion")
            appendLine("Release: $release")
            appendLine("Summary: $summary")
            appendLine("License: $license")
            appendLine("AutoReqProv: no")
            appendLine()
            appendLine("%description")
            appendLine(description)
            appendLine()
            appendLine("%install")
            appendLine("cp -a '${extractDir.absolutePath}'/* %{buildroot}/")
            appendLine()
            if (preIn.isNotEmpty()) {
                appendLine("%pre")
                appendLine(preIn)
                appendLine()
            }
            if (postIn.isNotEmpty()) {
                appendLine("%post")
                appendLine(postIn)
                appendLine()
            }
            if (preUn.isNotEmpty()) {
                appendLine("%preun")
                appendLine(preUn)
                appendLine()
            }
            if (postUn.isNotEmpty()) {
                appendLine("%postun")
                appendLine(postUn)
                appendLine()
            }
            appendLine("%files")
            append(fileEntries)
        }

        val specFile = File(workDir, "rebuild.spec")
        specFile.writeText(specContent)

        // Set up rpmbuild directory structure
        val rpmbuildDir = File(workDir, "rpmbuild")
        listOf("BUILD", "RPMS", "SOURCES", "SPECS", "SRPMS", "BUILDROOT").forEach {
            File(rpmbuildDir, it).mkdirs()
        }

        // Build new RPM with local rpmdb to avoid system rpmdb permission issues
        exec(
            "rpmbuild", "-bb",
            "--define", "_topdir ${rpmbuildDir.absolutePath}",
            "--define", "_dbpath ${localRpmDb.absolutePath}",
            "--target", arch,
            specFile.absolutePath
        )

        // Find the rebuilt RPM
        val builtRpm = File(rpmbuildDir, "RPMS/$arch").listFiles()
            ?.firstOrNull { it.extension == "rpm" }
            ?: error("Failed to find rebuilt RPM in ${rpmbuildDir.absolutePath}/RPMS/$arch")

        // Replace original with rebuilt RPM
        val originalName = rpmFile.name
        rpmFile.delete()
        builtRpm.copyTo(rpmFile)

        logger.lifecycle("[rpmPostProcess] Successfully rebuilt $originalName with .desktop file")
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
        logger.lifecycle("[rpmPostProcess] Created ${desktopFile.name}")
    }

    /**
     * Query RPM metadata. Stderr is discarded to avoid rpmdb warnings polluting the result.
     */
    private fun queryRpm(rpmFile: File, format: String): String {
        val process = ProcessBuilder("rpm", "-qp", "--queryformat", format, rpmFile.absolutePath)
            .redirectErrorStream(false)
            .start()
        // Discard stderr (rpmdb warnings)
        process.errorStream.bufferedReader().readText()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        return if (output == "(none)") "" else output
    }

    private fun exec(vararg args: String, workDir: File? = null) {
        val process = ProcessBuilder(*args)
            .apply { workDir?.let { directory(it) } }
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            error("Command failed with exit code $exitCode: ${args.joinToString(" ")}\n$output")
        }
    }
}
