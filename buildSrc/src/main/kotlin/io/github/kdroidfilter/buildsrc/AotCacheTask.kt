package io.github.kdroidfilter.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Generates an AOT (Ahead-of-Time) cache for a Compose Desktop distributable.
 *
 * This task:
 * 1. Locates the distributable app directory
 * 2. Provisions a java launcher in the bundled runtime (if needed)
 * 3. Records class loading by running the app for a training period
 * 4. Creates an AOT cache from the recorded profile
 * 5. Injects the AOT cache path into the app's .cfg file
 */
@DisableCachingByDefault(because = "AOT cache generation depends on runtime behavior")
abstract class AotCacheTask : DefaultTask() {

    @get:InputDirectory
    abstract val distributableDir: DirectoryProperty

    @get:Input
    abstract val aotCacheFileName: Property<String>

    @get:Input
    abstract val trainDurationSeconds: Property<Long>

    @get:Input
    abstract val toolchainJavaExe: Property<String>

    init {
        group = "compose desktop"
        aotCacheFileName.convention("aerodl.aot")
        trainDurationSeconds.convention(60L)
    }

    @TaskAction
    fun execute() {
        val baseDir = distributableDir.get().asFile
        val appDir = baseDir.listFiles()?.firstOrNull { it.isDirectory }
            ?: throw GradleException("Distributable app directory not found under $baseDir")

        logger.lifecycle("[aotCache] Processing ${appDir.name}")

        // Find app JAR directory (platform-specific)
        val appJarDir = findAppJarDir(appDir)

        // Provision java launcher in bundled runtime
        val (javaExe, cleanUpJava) = provisionJavaLauncher(appDir)

        try {
            // Parse .cfg file
            val cfgFile = appJarDir.listFiles()?.firstOrNull { it.extension == "cfg" }
                ?: throw GradleException("No .cfg file found in $appJarDir")
            val (classpath, javaOptions, mainClass) = parseCfgFile(cfgFile, appJarDir)

            // Generate AOT cache
            val aotCacheFile = File(appJarDir, aotCacheFileName.get())
            generateAotCache(javaExe, appDir, appJarDir, classpath, javaOptions, mainClass, aotCacheFile)

            // Inject AOT cache into .cfg
            injectAotCacheIntoCfg(cfgFile, aotCacheFileName.get())

            logger.lifecycle("[aotCache] Complete: ${aotCacheFile.absolutePath} (${aotCacheFile.length() / 1024}KB)")
        } finally {
            if (cleanUpJava) {
                logger.lifecycle("[aotCache] Keeping provisioned java launcher for --app-image packaging")
            }
        }
    }

    private fun findAppJarDir(appDir: File): File {
        return listOf(
            File(appDir, "Contents/app"),  // macOS
            File(appDir, "app"),           // Windows
            File(appDir, "lib/app")        // Linux
        ).firstOrNull { it.exists() }
            ?: throw GradleException("app/ subdirectory not found in $appDir")
    }

    private fun provisionJavaLauncher(appDir: File): Pair<String, Boolean> {
        val javaExePath = toolchainJavaExe.get()

        val runtimeHome = listOf(
            File(appDir, "Contents/runtime/Contents/Home"), // macOS
            File(appDir, "runtime"),                        // Windows
            File(appDir, "lib/runtime")                     // Linux
        ).firstOrNull { it.exists() }

        if (runtimeHome == null) {
            logger.warn("[aotCache] Bundled runtime not found, using toolchain java")
            return javaExePath to false
        }

        val runtimeBinDir = File(runtimeHome, "bin")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val exeName = if (isWindows) "java.exe" else "java"
        val provisionedJava = File(runtimeBinDir, exeName)

        // Check if launcher already exists
        if (provisionedJava.exists()) {
            return provisionedJava.absolutePath to false
        }

        // Provision java launcher
        runtimeBinDir.mkdirs()
        File(javaExePath).copyTo(provisionedJava, overwrite = true)
        provisionedJava.setExecutable(true)

        // Copy essential DLLs on Windows
        if (isWindows) {
            copyWindowsDlls(File(javaExePath).parentFile, runtimeBinDir)
        }

        logger.lifecycle("[aotCache] Provisioned java launcher at ${provisionedJava.absolutePath}")
        return provisionedJava.absolutePath to true
    }

    private fun copyWindowsDlls(toolchainBinDir: File, runtimeBinDir: File) {
        val essentialDlls = setOf(
            "jli.dll", "vcruntime140.dll", "msvcp140.dll", "ucrtbase.dll"
        )
        toolchainBinDir.listFiles()
            ?.filter { it.extension.lowercase() == "dll" && it.name.lowercase() in essentialDlls }
            ?.forEach { dll ->
                val target = File(runtimeBinDir, dll.name)
                if (!target.exists()) {
                    dll.copyTo(target, overwrite = false)
                }
            }
    }

    private data class CfgParseResult(
        val classpath: String,
        val javaOptions: List<String>,
        val mainClass: String
    )

    private fun parseCfgFile(cfgFile: File, appJarDir: File): CfgParseResult {
        val cpEntries = mutableListOf<String>()
        val javaOptions = mutableListOf<String>()
        var mainClass = ""
        var inClasspath = false
        var inJavaOptions = false

        for (line in cfgFile.readLines()) {
            val trimmed = line.trim()
            when {
                trimmed == "[JavaOptions]" -> { inJavaOptions = true; inClasspath = false }
                trimmed == "[ClassPath]" -> { inClasspath = true; inJavaOptions = false }
                trimmed == "[Application]" || trimmed == "[ArgOptions]" -> { inClasspath = false; inJavaOptions = false }
                trimmed.startsWith("app.mainclass=") -> mainClass = trimmed.substringAfter("app.mainclass=").trim()
                trimmed.startsWith("app.classpath=") -> cpEntries += trimmed.substringAfter("app.classpath=").trim()
                trimmed.startsWith("[") -> { inClasspath = false; inJavaOptions = false }
                inClasspath && trimmed.isNotEmpty() -> cpEntries += trimmed
                inJavaOptions && trimmed.isNotEmpty() -> {
                    val opt = if (trimmed.startsWith("java-options=")) trimmed.substringAfter("java-options=") else trimmed
                    if (!opt.contains("AOTCache")) {
                        javaOptions += opt.replace("\$APPDIR", appJarDir.absolutePath)
                    }
                }
            }
        }

        val classpath = cpEntries.joinToString(File.pathSeparator) { entry ->
            File(entry.replace("\$APPDIR", appJarDir.absolutePath)).absolutePath
        }

        return CfgParseResult(classpath, javaOptions, mainClass)
    }

    private fun generateAotCache(
        javaExe: String,
        appDir: File,
        appJarDir: File,
        classpath: String,
        javaOptions: List<String>,
        mainClass: String,
        aotCacheFile: File
    ) {
        val trainDuration = trainDurationSeconds.get()
        val aotConfigFile = File.createTempFile("aerodl-aot-", ".aotconf")

        val options = javaOptions.toMutableList()
        options += "-Daot.training.autoExit=$trainDuration"

        // Step 1: Record
        logger.lifecycle("[aotCache] Recording class loading profile (~${trainDuration}s)...")
        recordAotProfile(javaExe, appDir, classpath, options, mainClass, aotConfigFile, trainDuration)

        if (!aotConfigFile.exists() || aotConfigFile.length() == 0L) {
            throw GradleException("AOT configuration file was not created")
        }
        logger.lifecycle("[aotCache] Recorded ${aotConfigFile.length() / 1024}KB of profile data")

        // Step 2: Create cache
        logger.lifecycle("[aotCache] Creating cache...")
        createAotCache(javaExe, appDir, classpath, options, mainClass, aotConfigFile, aotCacheFile)

        aotConfigFile.delete()

        if (!aotCacheFile.exists()) {
            throw GradleException("AOT cache file was not created")
        }
    }

    private fun recordAotProfile(
        javaExe: String,
        appDir: File,
        classpath: String,
        javaOptions: List<String>,
        mainClass: String,
        aotConfigFile: File,
        trainDuration: Long
    ) {
        val args = mutableListOf(javaExe)
        args += listOf(
            "-XX:AOTMode=record",
            "-XX:AOTConfiguration=${aotConfigFile.absolutePath}",
            "-XX:+AOTClassLinking",  // Enable full AOT linking
            "-cp", classpath
        )
        args += javaOptions
        args += mainClass

        val logFile = File.createTempFile("aerodl-aot-record-", ".log")
        val processBuilder = ProcessBuilder(args)
            .directory(appDir)
            .redirectErrorStream(true)
            .redirectOutput(logFile)

        // Handle headless Linux with Xvfb
        val os = System.getProperty("os.name").lowercase()
        val isLinux = os.contains("linux")
        val needsXvfb = isLinux && System.getenv("DISPLAY").isNullOrEmpty()

        var xvfbProcess: Process? = null
        if (needsXvfb) {
            val display = ":99"
            xvfbProcess = ProcessBuilder("Xvfb", display, "-screen", "0", "1280x1024x24")
                .redirectErrorStream(true)
                .start()
            Thread.sleep(1000)
            processBuilder.environment()["DISPLAY"] = display
            logger.lifecycle("[aotCache] Started Xvfb on $display")
        }

        val process = processBuilder.start()

        // Wait for app to self-terminate
        val deadline = System.currentTimeMillis() + (trainDuration + 30) * 1000
        while (process.isAlive && System.currentTimeMillis() < deadline) {
            Thread.sleep(500)
        }
        if (process.isAlive) {
            logger.warn("[aotCache] App did not self-terminate, forcing kill")
            process.destroyForcibly()
        }

        val exitCode = process.waitFor()
        xvfbProcess?.destroyForcibly()

        // Log output for debugging
        val output = logFile.readText().takeLast(3000)
        if (output.isNotBlank()) {
            logger.lifecycle("[aotCache] Record output (exit $exitCode):\n$output")
        }
        logFile.delete()

        // Check for JVM crash dumps
        appDir.listFiles()?.filter { it.name.startsWith("hs_err_pid") }?.forEach { hsErr ->
            logger.lifecycle("[aotCache] JVM crash dump: ${hsErr.name}")
            logger.lifecycle(hsErr.readText().take(2000))
            hsErr.delete()
        }
    }

    private fun createAotCache(
        javaExe: String,
        appDir: File,
        classpath: String,
        javaOptions: List<String>,
        mainClass: String,
        aotConfigFile: File,
        aotCacheFile: File
    ) {
        val args = mutableListOf(javaExe)
        args += listOf(
            "-XX:AOTMode=create",
            "-XX:AOTConfiguration=${aotConfigFile.absolutePath}",
            "-XX:AOTCache=${aotCacheFile.absolutePath}",
            "-XX:+AOTClassLinking",  // Enable full AOT linking for faster startup
            "-cp", classpath
        )
        args += javaOptions
        args += mainClass

        val process = ProcessBuilder(args)
            .directory(appDir)
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("AOT cache creation failed with exit code $exitCode")
        }
    }

    private fun injectAotCacheIntoCfg(cfgFile: File, aotCacheFileName: String) {
        val content = cfgFile.readText()
        if (!content.contains("AOTCache")) {
            val updatedContent = content.replace(
                "[JavaOptions]",
                "[JavaOptions]\njava-options=-XX:AOTCache=\$APPDIR/$aotCacheFileName"
            )
            cfgFile.writeText(updatedContent)
            logger.lifecycle("[aotCache] Injected AOTCache into ${cfgFile.name}")
        }
    }
}
