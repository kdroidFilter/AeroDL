package io.github.kdroidfilter.buildsrc

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Configuration for .rpm post-processing.
 */
data class RpmPostProcessConfig(
    val packageName: String = "aerodl",
    val appName: String = "AeroDl",
    val execPath: String = "/opt/aerodl/bin/AeroDl",
    val iconPath: String = "/opt/aerodl/lib/AeroDl.png",
    val startupWMClass: String? = null,
    val categories: String = "AudioVideo;Audio;Video;Network;"
)

/**
 * Helper to register .rpm post-processing tasks.
 */
object RpmPostProcessHelper {

    /**
     * Registers a task to post-process a .rpm file.
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
        config: RpmPostProcessConfig = RpmPostProcessConfig()
    ): TaskProvider<RpmPostProcessTask> {
        return project.tasks.register(taskName, RpmPostProcessTask::class.java) {
            rpmDirectory.set(project.layout.buildDirectory.dir("compose/binaries/$buildType/rpm"))
            packageName.set(config.packageName)
            appName.set(config.appName)
            execPath.set(config.execPath)
            iconPath.set(config.iconPath)
            config.startupWMClass?.let { startupWMClass.set(it) }
            categories.set(config.categories)
        }
    }
}
