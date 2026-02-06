package io.github.kdroidfilter.buildsrc

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaToolchainService

/**
 * Configuration for AOT cache generation.
 */
data class AotCacheConfig(
    val aotCacheFileName: String = "aerodl.aot",
    val trainDurationSeconds: Long = 60L
)

/**
 * Helper to register AOT cache generation tasks.
 */
object AotCacheHelper {

    /**
     * Registers a task to generate an AOT cache for a distributable.
     *
     * @param project The Gradle project
     * @param taskName Name for the registered task
     * @param dependsOnTask Task that creates the distributable
     * @param binariesSubdir Subdirectory under compose/binaries (e.g., "main" or "main-release")
     * @param config Configuration for AOT generation
     * @return The registered task provider
     */
    fun registerTask(
        project: Project,
        taskName: String,
        dependsOnTask: String,
        binariesSubdir: String,
        config: AotCacheConfig = AotCacheConfig()
    ): TaskProvider<AotCacheTask> {
        // Resolve toolchain java path at configuration time (configuration cache compatible)
        val javaToolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        val javaToolchainService = project.extensions.getByType(JavaToolchainService::class.java)
        val javaLauncher = javaToolchainService.launcherFor(javaToolchain)

        return project.tasks.register(taskName, AotCacheTask::class.java) {
            description = "Generate an AOT cache for the $binariesSubdir distributable"
            dependsOn(project.tasks.named(dependsOnTask))
            distributableDir.set(project.layout.buildDirectory.dir("compose/binaries/$binariesSubdir/app"))
            aotCacheFileName.set(config.aotCacheFileName)
            trainDurationSeconds.set(config.trainDurationSeconds)
            toolchainJavaExe.set(javaLauncher.map { it.executablePath.asFile.absolutePath })
        }
    }
}
