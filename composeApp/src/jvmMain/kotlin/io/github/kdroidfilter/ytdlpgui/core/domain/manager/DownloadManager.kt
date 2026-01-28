@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.core.domain.manager

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import dev.zacsweers.metro.Inject
import io.github.kdroidfilter.knotify.builder.ExperimentalNotificationsApi
import io.github.kdroidfilter.knotify.compose.builder.notification
import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ffmpeg.core.ConversionEvent
import io.github.kdroidfilter.ffmpeg.core.ConversionHandle
import io.github.kdroidfilter.ffmpeg.core.ConversionOptions
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.DownloadSection
import io.github.kdroidfilter.ytdlp.core.Event
import io.github.kdroidfilter.ytdlp.core.Handle
import io.github.kdroidfilter.ytdlp.core.SubtitleOptions
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.config.SettingsKeys
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.core.platform.notifications.NotificationThumbUtils
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.logging.warnln
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import io.github.kdroidfilter.ytdlpgui.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.Res
import ytdlpgui.composeapp.generated.resources.conversion_completed_message
import ytdlpgui.composeapp.generated.resources.conversion_completed_title
import ytdlpgui.composeapp.generated.resources.download_completed_message
import ytdlpgui.composeapp.generated.resources.download_completed_title
import ytdlpgui.composeapp.generated.resources.open_directory
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.collections.ArrayDeque

/**
 * Manages downloading of videos and audio from URLs using the `YtDlpWrapper`. It keeps track of
 * download progress, handles download-related events, and maintains a queue for pending downloads.
 *
 * @constructor Creates an instance of `DownloadManager`.
 * @param ytDlpWrapper The YtDlpWrapper instance used for performing downloads.
 * @param settings Provides settings for configuring the maximum parallel downloads and other options.
 * @param historyRepository Stores the history of completed downloads.
 * @param trayAppState The tray application state for managing window visibility.
 */
/**
 * Task type enum to differentiate between downloads and conversions.
 */
enum class TaskType {
    DOWNLOAD,
    CONVERSION
}

@Inject
class DownloadManager(
    private val ytDlpWrapper: YtDlpWrapper,
    private val ffmpegWrapper: FfmpegWrapper,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: DownloadHistoryRepository,
    private val trayAppState: TrayAppState,
) {
    private val scope = CoroutineScope(Dispatchers.IO)


    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val taskType: TaskType = TaskType.DOWNLOAD,
        val url: String = "",
        val videoInfo: VideoInfo? = null,
        val preset: YtDlpWrapper.Preset? = null,
        val splitChapters: Boolean = false,
        val sponsorBlock: Boolean = false,
        val progress: Float = 0f,
        val speedBytesPerSec: Long? = null,
        val status: Status = Status.Pending,
        val message: String? = null,
        val handle: Handle? = null,
        val subtitleLanguages: List<String>? = null,
        val audioQualityPreset: YtDlpWrapper.AudioQualityPreset? = null,
        // Trim/cut section (downloads only the specified time range)
        val downloadSection: DownloadSection? = null,
        // Conversion-specific fields
        val inputFile: File? = null,
        val outputFile: File? = null,
        val conversionHandle: ConversionHandle? = null,
        val outputFormat: String? = null,
        val totalDuration: java.time.Duration? = null,
    ) {
        enum class Status { Pending, Running, Completed, Failed, Cancelled }

        /** Display name for UI: video title, file name, or URL */
        val displayName: String
            get() = when (taskType) {
                TaskType.DOWNLOAD -> videoInfo?.title ?: url
                TaskType.CONVERSION -> {
                    val fileName = inputFile?.name ?: "File"
                    val format = outputFormat ?: "?"
                    "$fileName → $format"
                }
            }
    }

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    // Reactive global state: true if at least one download is currently running
    val isDownloading: StateFlow<Boolean> = _items
        .map { list -> list.any { it.status == DownloadItem.Status.Running } }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val pendingQueue: ArrayDeque<String> = ArrayDeque()

    private fun maxParallel(): Int = settingsRepository.parallelDownloads.value
    private fun runningCount(): Int = _items.value.count { it.status == DownloadItem.Status.Running }

    fun start(
        url: String,
        videoInfo: VideoInfo? = null,
        preset: YtDlpWrapper.Preset? = null,
        sponsorBlock: Boolean = false,
        downloadSection: DownloadSection? = null
    ): String = enqueueDownload(url, videoInfo, preset ?: YtDlpWrapper.Preset.P720, null, null, splitChapters = false, sponsorBlock = sponsorBlock, downloadSection = downloadSection)

    fun startWithSubtitles(
        url: String,
        videoInfo: VideoInfo? = null,
        preset: YtDlpWrapper.Preset? = null,
        languages: List<String>,
        sponsorBlock: Boolean = false,
        downloadSection: DownloadSection? = null
    ): String = enqueueDownload(url, videoInfo, preset ?: YtDlpWrapper.Preset.P720, languages.filter { it.isNotBlank() }, null, splitChapters = false, sponsorBlock = sponsorBlock, downloadSection = downloadSection)

    fun startAudio(
        url: String,
        videoInfo: VideoInfo? = null,
        audioQualityPreset: YtDlpWrapper.AudioQualityPreset? = null,
        sponsorBlock: Boolean = false,
        downloadSection: DownloadSection? = null
    ): String = enqueueDownload(url, videoInfo, null, null, audioQualityPreset, splitChapters = false, sponsorBlock = sponsorBlock, downloadSection = downloadSection)

    fun startAudioSplitChapters(
        url: String,
        videoInfo: VideoInfo? = null,
        audioQualityPreset: YtDlpWrapper.AudioQualityPreset? = null,
        sponsorBlock: Boolean = false
    ): String = enqueueDownload(url, videoInfo, null, null, audioQualityPreset, splitChapters = true, sponsorBlock = sponsorBlock, downloadSection = null)

    fun startSplitChapters(
        url: String,
        videoInfo: VideoInfo? = null,
        preset: YtDlpWrapper.Preset? = null,
        languages: List<String>? = null,
        sponsorBlock: Boolean = false,
    ): String = enqueueDownload(
        url = url,
        videoInfo = videoInfo,
        preset = preset ?: YtDlpWrapper.Preset.P720,
        subtitles = languages?.filter { it.isNotBlank() },
        audioQualityPreset = null,
        splitChapters = true,
        sponsorBlock = sponsorBlock,
        downloadSection = null
    )

    /**
     * Starts a file conversion task.
     * @param inputFile The source file to convert
     * @param outputFile The destination file
     * @param options Conversion options (codec, quality, etc.)
     * @param totalDuration Duration of the input file for progress calculation
     * @return The task ID
     */
    fun startConversion(
        inputFile: File,
        outputFile: File,
        options: ConversionOptions,
        totalDuration: java.time.Duration? = null
    ): String = enqueueConversion(inputFile, outputFile, options, totalDuration)

    private fun enqueueConversion(
        inputFile: File,
        outputFile: File,
        options: ConversionOptions,
        totalDuration: java.time.Duration?
    ): String {
        val outputFormat = outputFile.extension.uppercase()
        val item = DownloadItem(
            taskType = TaskType.CONVERSION,
            inputFile = inputFile,
            outputFile = outputFile,
            outputFormat = outputFormat,
            totalDuration = totalDuration
        )
        // Store options in a temporary map for retrieval during launch
        conversionOptionsMap[item.id] = options
        _items.value += item
        pendingQueue.addLast(item.id)
        maybeStartPending()
        return item.id
    }

    // Temporary storage for conversion options (cleared after launch)
    private val conversionOptionsMap = mutableMapOf<String, ConversionOptions>()

    private fun enqueueDownload(
        url: String,
        videoInfo: VideoInfo?,
        preset: YtDlpWrapper.Preset?,
        subtitles: List<String>?,
        audioQualityPreset: YtDlpWrapper.AudioQualityPreset? = null,
        splitChapters: Boolean = false,
        sponsorBlock: Boolean = false,
        downloadSection: DownloadSection? = null
    ): String {
        val item = DownloadItem(
            url = url,
            videoInfo = videoInfo,
            preset = preset,
            splitChapters = splitChapters,
            sponsorBlock = sponsorBlock,
            subtitleLanguages = subtitles,
            audioQualityPreset = audioQualityPreset,
            downloadSection = downloadSection
        )
        _items.value += item
        pendingQueue.addLast(item.id)
        maybeStartPending()
        return item.id
    }

    fun cancel(id: String) {
        pendingQueue.remove(id)
        val item = _items.value.find { it.id == id }
        item?.handle?.cancel()
        item?.conversionHandle?.cancel()
        update(id) { it.copy(status = DownloadItem.Status.Cancelled) }
        maybeStartPending()
    }

    /**
     * Removes a task item from the in-memory list. If the item is currently running,
     * its handle is cancelled first. Also ensures it is removed from the pending queue.
     */
    fun remove(id: String) {
        pendingQueue.remove(id)
        // Cancel if still running
        val item = _items.value.find { it.id == id }
        item?.handle?.cancel()
        item?.conversionHandle?.cancel()
        // Drop the item from the list
        _items.value = _items.value.filterNot { it.id == id }
        conversionOptionsMap.remove(id)
        maybeStartPending()
    }

    private fun maybeStartPending() {
        while (runningCount() < maxParallel() && pendingQueue.isNotEmpty()) {
            val id = pendingQueue.removeFirst()
            val item = _items.value.find { it.id == id }
            when (item?.taskType) {
                TaskType.DOWNLOAD -> launchDownload(id)
                TaskType.CONVERSION -> launchConversion(id)
                null -> {} // Item not found, skip
            }
        }
    }

    private fun launchConversion(id: String) {
        val item = _items.value.find { it.id == id } ?: return
        if (item.status != DownloadItem.Status.Pending) return
        if (item.taskType != TaskType.CONVERSION) return

        val inputFile = item.inputFile ?: return
        val outputFile = item.outputFile ?: return
        val options = conversionOptionsMap.remove(id) ?: return

        // Mark as Running IMMEDIATELY so that runningCount() reflects this task
        // before any async callbacks. This ensures maxParallel is respected.
        update(id) { it.copy(status = DownloadItem.Status.Running) }

        val conversionHandle = ffmpegWrapper.convert(
            inputFile = inputFile,
            outputFile = outputFile,
            options = options
        ) { event ->
            when (event) {
                is ConversionEvent.Started -> {
                    infoln { "[DownloadManager] Conversion started for item $id" }
                }
                is ConversionEvent.Progress -> {
                    // Calculate progress using stored totalDuration from media analysis
                    val totalMs = item.totalDuration?.toMillis()?.toFloat()
                    val processedMs = event.timeProcessed.toMillis().toFloat()

                    if (totalMs != null && totalMs > 0) {
                        val progress = (processedMs / totalMs * 100f).coerceIn(0f, 100f)
                        update(id) { it.copy(progress = progress) }
                    }
                }
                is ConversionEvent.Log -> {
                    // Optionally log for debugging
                }
                is ConversionEvent.Completed -> {
                    infoln { "[DownloadManager] Conversion completed for item $id: ${event.outputFile.absolutePath}" }
                    update(id) {
                        it.copy(
                            status = DownloadItem.Status.Completed,
                            progress = 100f,
                            outputFile = event.outputFile
                        )
                    }
                    // Save to history
                    saveConversionToHistory(id, item, event.outputFile.absolutePath)

                    // Notify when enabled in settings and window is hidden
                    if (settingsRepository.notifyOnComplete.value && !trayAppState.isVisible.value) {
                        scope.launch { sendConversionCompletionNotification(item, event.outputFile.absolutePath) }
                    }

                    // Remove completed conversion from memory after a short delay
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        remove(id)
                    }
                }
                is ConversionEvent.Error -> {
                    errorln { "[DownloadManager] Conversion error for item $id: ${event.message}" }
                    update(id) {
                        it.copy(
                            status = DownloadItem.Status.Failed,
                            message = event.message
                        )
                    }
                    maybeStartPending()
                }
                is ConversionEvent.Cancelled -> {
                    infoln { "[DownloadManager] Conversion cancelled for item $id" }
                    update(id) { it.copy(status = DownloadItem.Status.Cancelled) }
                    remove(id)
                }
            }
        }

        update(id) { it.copy(conversionHandle = conversionHandle) }
    }

    private fun launchDownload(id: String) {
        val item = _items.value.find { it.id == id } ?: return
        if (item.status != DownloadItem.Status.Pending) return

        // Mark as Running IMMEDIATELY so that runningCount() reflects this task
        // before any async callbacks. This ensures maxParallel is respected.
        update(id) { it.copy(status = DownloadItem.Status.Running) }

        // Fallback trackers from logs (kept, but strengthened)
        var lastDestPath: String? = null
        val rDest = Regex("(?i)^\\[download]\\s+Destination:\\s+(.+)$")
        val rMerging = Regex("(?i)^\\[(?:Merger|Fixup.*)]\\s+Merging formats into\\s+\"(.+)\"$")
        val rMoving  = Regex("(?i)^\\[(?:download|move)]\\s+Moving to\\s+\"(.+)\"$")

        // NEW: predictable temp sink based on URL MD5 (must match buildCommand)
        val sinkFile = File(System.getProperty("java.io.tmpdir"), "ytdlp-finalpath-${md5(item.url)}.txt")
        // Clean any stale file from previous run
        runCatching { if (sinkFile.exists()) sinkFile.delete() }

        val eventHandler = createEventHandler(
            id = id,
            item = item,
            getOutputPath = { lastDestPath }, // logs fallback (kept)
            onLog = { line ->
                rDest.find(line)?.let { lastDestPath = it.groupValues[1].trim('"') }
                rMerging.find(line)?.let { lastDestPath = it.groupValues[1].trim('"') }
                rMoving.find(line)?.let  { lastDestPath = it.groupValues[1].trim('"') }
            },
            finalPathSink = sinkFile // pass to completion to read file
        )

        val handle = when {
            item.preset == null && item.splitChapters -> downloadAudioSplitChapters(item, eventHandler)
            item.preset == null -> downloadAudio(item, eventHandler)
            item.splitChapters -> downloadVideoSplitChapters(item, eventHandler)
            !item.subtitleLanguages.isNullOrEmpty() -> downloadVideoWithSubtitles(item, eventHandler)
            else -> downloadVideo(item, eventHandler)
        }

        update(id) { it.copy(handle = handle) }
    }

    private fun createEventHandler(
        id: String,
        item: DownloadItem,
        getOutputPath: () -> String?,
        onLog: (String) -> Unit,
        finalPathSink: File
    ): (Event) -> Unit = { event ->
        when (event) {
            is Event.Started -> {
                infoln { "[DownloadManager] Download started for item $id" }
                update(id) { it.copy(status = DownloadItem.Status.Running, message = null) }
            }

            is Event.Progress -> {
                val pct = (event.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
                update(id) { it.copy(progress = pct, speedBytesPerSec = event.speedBytesPerSec, message = null) }
            }

            is Event.Log -> onLog(event.line)

            is Event.Completed -> {
                val status = if (event.success) DownloadItem.Status.Completed else DownloadItem.Status.Failed
                infoln { "[DownloadManager] Download completed for item $id, success=${event.success}" }
                // Only clear message on success - preserve error message on failure
                update(id) {
                    if (event.success) {
                        it.copy(status = status, message = null)
                    } else {
                        it.copy(status = status)
                    }
                }

                if (event.success) {
                    infoln { "[DownloadManager] Reading final path from sink: ${finalPathSink.absolutePath}" }
                    val absolutePath =
                        readFinalPathFromSink(finalPathSink)
                            ?: computeAbsoluteFromFallbacks(item, getOutputPath())

                    if (absolutePath != null) {
                        infoln { "[DownloadManager] Final output path: $absolutePath" }
                    } else {
                        warnln { "[DownloadManager] Could not determine final output path for item $id" }
                    }

                    // If split-chapters, delete the non-split base file (keep only chapter files)
                    if (item.splitChapters) {
                        deleteBaseFileIfSplit(item, finalPathSink)
                    }

                    saveToHistory(id, item, absolutePath)

                    // Notify when enabled in settings and window is hidden
                    if (settingsRepository.notifyOnComplete.value && !trayAppState.isVisible.value) {
                        scope.launch { sendCompletionNotification(item, absolutePath) }
                    }
                }

                // Cleanup sink whatever the result
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }

                // Only remove from memory when the download actually succeeded.
                // Keep failed items so the user can see the error and dismiss manually.
                if (event.success) {
                    // Drop completed items to prevent list growth (history persists them)
                    remove(id)
                } else {
                    // Ensure pending queue advances after a failure as well
                    maybeStartPending()
                }
            }

            is Event.Error -> {
                errorln { "[DownloadManager] Download error for item $id: ${event.message}" }
                event.cause?.let { errorln { "[DownloadManager] Error cause: ${it.message}" } }
                update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.message) }
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }
                maybeStartPending()
            }

            is Event.Cancelled -> {
                infoln { "[DownloadManager] Download cancelled for item $id" }
                update(id) { it.copy(status = DownloadItem.Status.Cancelled, message = null) }
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }
                // Remove cancelled items to avoid leaking them in memory
                remove(id)
            }

            is Event.NetworkProblem -> {
                errorln { "[DownloadManager] Network problem for item $id: ${event.detail}" }
                update(id) { it.copy(status = DownloadItem.Status.Failed, message = event.detail) }
                runCatching { if (finalPathSink.exists()) finalPathSink.delete() }
                maybeStartPending()
            }
        }
    }

    private fun readFinalPathFromSink(file: File): String? {
        return try {
            if (!file.exists()) {
                null
            } else {
                // Keep last non-blank line (handles playlists / multiple outputs appending)
                file.readLines()
                    .asReversed()
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?.trim('"')
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readAllPathsFromSink(file: File): List<String> = try {
        if (!file.exists()) emptyList()
        else file.readLines().mapNotNull { line ->
            val p = line.trim().trim('"')
            if (p.isBlank()) null else p
        }
    } catch (_: Exception) { emptyList() }

    private fun deleteBaseFileIfSplit(item: DownloadItem, finalPathSink: File) {
        val outputs = readAllPathsFromSink(finalPathSink)
        if (outputs.isEmpty()) return

        val ext = if (item.preset == null) "mp3" else "mp4"
        val candidates = outputs.filter { path ->
            path.endsWith(".$ext", ignoreCase = true) &&
                    // Exclude chapter files that live under a directory named like a file with extension
                    (File(path).parentFile?.name?.endsWith(".$ext", ignoreCase = true) != true)
        }
        val baseToDelete = candidates.lastOrNull() ?: return
        runCatching {
            val f = File(baseToDelete)
            if (f.exists()) {
                val ok = f.delete()
                if (ok) infoln { "[DownloadManager] Deleted base file after split: ${f.absolutePath}" }
                else warnln { "[DownloadManager] Failed to delete base file after split: ${f.absolutePath}" }
            }
        }
    }

    private fun computeAbsoluteFromFallbacks(item: DownloadItem, loggedPath: String?): String? {
        loggedPath?.let {
            val p = File(it.trim('"'))
            if (p.isAbsolute) return p.absolutePath
            ytDlpWrapper.downloadDir?.let { dir -> return File(dir, p.path).absolutePath }
            return p.absolutePath
        }

        // Final fallback: synthesize from title + preset (kept from your logic)
        val downloadDir = ytDlpWrapper.downloadDir ?: return null
        val videoTitle = item.videoInfo?.title ?: return null
        val safeTitle = sanitizeFilename(videoTitle)
        val ext = if (item.preset == null) "mp3" else "mp4"
        return File(downloadDir.absolutePath, "$safeTitle.$ext").absolutePath
    }

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }


    /** Builds extra args including sponsorblock and download section (trim) if applicable */
    private fun buildExtraArgs(item: DownloadItem): List<String> = buildList {
        if (item.sponsorBlock) {
            add("--sponsorblock-remove")
            add("default")
        }
        item.downloadSection?.let { section ->
            add("--download-sections")
            add(section.toYtDlpFormat())
            add("--force-keyframes-at-cuts")
            infoln { "[DownloadManager] Adding trim section: ${section.toYtDlpFormat()}" }
        }
    }

    private fun downloadAudio(item: DownloadItem, onEvent: (Event) -> Unit): Handle =
        ytDlpWrapper.downloadAudioMp3WithPreset(
            url = item.url,
            preset = item.audioQualityPreset ?: YtDlpWrapper.AudioQualityPreset.HIGH,
            outputTemplate = buildOutputTemplateForAudio(item.audioQualityPreset),
            extraArgs = buildExtraArgs(item),
            onEvent = onEvent
        )

    private fun downloadVideo(item: DownloadItem, onEvent: (Event) -> Unit): Handle =
        ytDlpWrapper.downloadMp4At(
            url = item.url,
            preset = item.preset ?: YtDlpWrapper.Preset.P720,
            outputTemplate = buildOutputTemplate(item.preset),
            extraArgs = buildExtraArgs(item),
            onEvent = onEvent
        )

    private fun downloadAudioSplitChapters(item: DownloadItem, onEvent: (Event) -> Unit): Handle {
        val directoryTemplate = buildDirectoryTemplateForAudio(item.audioQualityPreset)
        val splitFileTemplate = buildOutputTemplateForAudioSplitChapters(item.audioQualityPreset)
        val dir = ytDlpWrapper.downloadDir
        val chapterTemplate = if (dir != null) {
            val basePath = File(dir, directoryTemplate).absolutePath
            "chapter:$basePath/$splitFileTemplate"
        } else {
            "chapter:$directoryTemplate/$splitFileTemplate"
        }

        return ytDlpWrapper.downloadAudioMp3WithPreset(
            url = item.url,
            preset = item.audioQualityPreset ?: YtDlpWrapper.AudioQualityPreset.HIGH,
            // Do NOT set a base output; only chapter outputs
            outputTemplate = null,
            extraArgs = buildList {
                add("--split-chapters"); add("-o"); add(chapterTemplate)
                if (item.sponsorBlock) { add("--sponsorblock-remove"); add("default") }
            },
            onEvent = onEvent
        )
    }

    private fun downloadVideoSplitChapters(item: DownloadItem, onEvent: (Event) -> Unit): Handle {
        val subtitleOptions = item.subtitleLanguages?.let { langs ->
            if (langs.isNotEmpty()) SubtitleOptions(
                languages = langs,
                writeAutoSubtitles = true,
                embedSubtitles = true,
                writeSubtitles = false,
                subFormat = "srt"
            ) else null
        }

        val directoryTemplate = buildDirectoryTemplateForVideo(item.preset)
        val splitFileTemplate = buildOutputTemplateForSplitChapters(item.preset)
        val chapterTemplate = "chapter:$directoryTemplate/$splitFileTemplate"

        return ytDlpWrapper.downloadMp4SplitChapters(
            url = item.url,
            preset = item.preset ?: YtDlpWrapper.Preset.P720,
            // Do NOT set a base output; only chapter outputs
            outputTemplate = null,
            extraArgs = buildList {
                add("-o"); add(chapterTemplate)
                if (item.sponsorBlock) { add("--sponsorblock-remove"); add("default") }
            },
            subtitles = subtitleOptions,
            onEvent = onEvent
        )
    }

    private fun downloadVideoWithSubtitles(item: DownloadItem, onEvent: (Event) -> Unit): Handle {
        infoln { "[DownloadManager] Initiating video download with subtitles for: ${item.url}" }
        infoln { "[DownloadManager] Requested subtitle languages: ${item.subtitleLanguages?.joinToString(",") ?: "none"}" }
        infoln { "[DownloadManager] Preset: ${item.preset?.height}p" }

        val subtitleOptions = SubtitleOptions(
            languages = item.subtitleLanguages ?: emptyList(),
            writeAutoSubtitles = true,
            embedSubtitles = true,
            writeSubtitles = false,
            subFormat = "srt"
        )

        infoln { "[DownloadManager] SubtitleOptions: embed=${subtitleOptions.embedSubtitles}, writeAuto=${subtitleOptions.writeAutoSubtitles}, write=${subtitleOptions.writeSubtitles}" }

        return ytDlpWrapper.downloadMp4At(
            url = item.url,
            preset = item.preset ?: YtDlpWrapper.Preset.P720,
            outputTemplate = buildOutputTemplate(item.preset),
            extraArgs = buildExtraArgs(item),
            subtitles = subtitleOptions,
            onEvent = onEvent
        )
    }

    private fun buildOutputTemplate(preset: YtDlpWrapper.Preset?): String {
        val includePreset = settingsRepository.includePresetInFilename.value
        return if (includePreset && preset != null) {
            "%(title)s_${preset.height}p.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }
    }

    private fun buildOutputTemplateForAudio(preset: YtDlpWrapper.AudioQualityPreset?): String {
        val includePreset = settingsRepository.includePresetInFilename.value
        return if (includePreset && preset != null) {
            "%(title)s_${preset.bitrate}.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }
    }

    private fun buildOutputTemplateForSplitChapters(preset: YtDlpWrapper.Preset?): String {
        val includePreset = settingsRepository.includePresetInFilename.value
        return if (includePreset && preset != null) {
            "%(title)s_%(section_number)02d_%(section_title)s_${preset.height}p.%(ext)s"
        } else {
            "%(title)s_%(section_number)02d_%(section_title)s.%(ext)s"
        }
    }

    private fun buildOutputTemplateForAudioSplitChapters(preset: YtDlpWrapper.AudioQualityPreset?): String {
        val includePreset = settingsRepository.includePresetInFilename.value
        return if (includePreset && preset != null) {
            "%(title)s_%(section_number)02d_%(section_title)s_${preset.bitrate}.%(ext)s"
        } else {
            "%(title)s_%(section_number)02d_%(section_title)s.%(ext)s"
        }
    }

    private fun buildDirectoryTemplateForVideo(preset: YtDlpWrapper.Preset?): String {
        val includePreset = settingsRepository.includePresetInFilename.value
        return if (includePreset && preset != null) {
            "%(title)s_${preset.height}p"
        } else {
            "%(title)s"
        }
    }

    private fun buildDirectoryTemplateForAudio(preset: YtDlpWrapper.AudioQualityPreset?): String {
        val includePreset = settingsRepository.includePresetInFilename.value
        return if (includePreset && preset != null) {
            "%(title)s_${preset.bitrate}"
        } else {
            "%(title)s"
        }
    }

    private fun saveToHistory(id: String, item: DownloadItem, outputFilePath: String?) {
        historyRepository.add(
            id = id,
            url = item.url,
            videoInfo = item.videoInfo,
            outputPath = outputFilePath,
            isAudio = item.preset == null,
            isSplit = item.splitChapters,
            presetHeight = item.preset?.height,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun saveConversionToHistory(id: String, item: DownloadItem, outputFilePath: String?) {
        historyRepository.add(
            id = id,
            url = item.inputFile?.absolutePath ?: "",
            videoInfo = null,
            outputPath = outputFilePath,
            isAudio = item.outputFormat?.equals("MP3", ignoreCase = true) == true,
            isSplit = false,
            presetHeight = null,
            createdAt = System.currentTimeMillis()
        )
    }

    @OptIn(ExperimentalNotificationsApi::class)
    private suspend fun sendConversionCompletionNotification(item: DownloadItem, absolutePath: String?) {
        val title = getString(Res.string.conversion_completed_title)
        val fileName = absolutePath?.let { File(it).name } ?: item.inputFile?.name ?: "File"
        val message = getString(Res.string.conversion_completed_message, fileName)
        val openBtn = getString(Res.string.open_directory)

        fun openDirAction() { absolutePath?.let { FileExplorerUtils.openDirectoryForPath(it) } }

        val notif = notification(
            title = title,
            message = message,
            onActivated = { openDirAction() },
            onDismissed = { },
            onFailed = { }
        ) {
            button(title = openBtn) { openDirAction() }
        }
        notif.send()
    }

    @OptIn(ExperimentalNotificationsApi::class)
    private suspend fun sendCompletionNotification(item: DownloadItem, absolutePath: String?) {
        val title = getString(Res.string.download_completed_title)
        val nameOrUrl = item.videoInfo?.title?.ifBlank { null } ?: run {
            absolutePath?.let { File(it).nameWithoutExtension }
        } ?: item.url
        val message = getString(Res.string.download_completed_message, nameOrUrl)
        val openBtn = getString(Res.string.open_directory)

        fun openDirAction() { absolutePath?.let { FileExplorerUtils.openDirectoryForPath(it) } }

        val thumbUrl = NotificationThumbUtils.resolveThumbnailUrl(item.videoInfo?.thumbnail, item.url)
        val largeIconContent = NotificationThumbUtils.buildLargeIcon(thumbUrl)

        val notif = notification(
            title = title,
            message = message,
            largeIcon = largeIconContent,
            onActivated = { openDirAction() },
            onDismissed = { },
            onFailed = { }
        ) {
            button(title = openBtn) { openDirAction() }
        }
        notif.send()
    }

    private fun update(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.value = _items.value.map {
            if (it.id == id) {
                val updated = transform(it)
                if (updated.status == DownloadItem.Status.Failed) {
                    infoln { "[DownloadManager] Updating item $id to Failed status with message: ${updated.message}" }
                }
                updated
            } else it
        }
    }

    private  fun sanitizeFilename(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
}
