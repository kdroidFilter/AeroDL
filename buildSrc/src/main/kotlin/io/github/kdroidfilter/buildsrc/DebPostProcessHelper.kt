package io.github.kdroidfilter.buildsrc

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Configuration for .deb post-processing.
 */
data class DebPostProcessConfig(
    val packageName: String = "aerodl",
    val appName: String = "AeroDl",
    val execPath: String = "/opt/aerodl/bin/AeroDl",
    val iconPath: String = "/opt/aerodl/lib/AeroDl.png",
    val startupWMClass: String? = null,
    val categories: String = "AudioVideo;Audio;Video;Network;",
    val enableT64AlternativeDeps: Boolean = true
)

/**
 * Helper to register .deb post-processing tasks.
 */
object DebPostProcessHelper {

    /**
     * Registers a task to post-process a .deb file.
     *
     * @param project The Gradle project
     * @param taskName Name for the registered task
     * @param buildType Build type path (e.g., "main-release" or "main")
     * @param config Configuration for the post-processing
     * @return The registered task provider
     */
    fun registerTask(
        project: Project,
        taskName: String,
        buildType: String,
        config: DebPostProcessConfig = DebPostProcessConfig()
    ): TaskProvider<DebPostProcessTask> {
        return project.tasks.register(taskName, DebPostProcessTask::class.java) {
            debDirectory.set(project.layout.buildDirectory.dir("compose/binaries/$buildType/deb"))
            packageName.set(config.packageName)
            appName.set(config.appName)
            execPath.set(config.execPath)
            iconPath.set(config.iconPath)
            config.startupWMClass?.let { startupWMClass.set(it) }
            categories.set(config.categories)
            enableT64AlternativeDeps.set(config.enableT64AlternativeDeps)
        }
    }
}
