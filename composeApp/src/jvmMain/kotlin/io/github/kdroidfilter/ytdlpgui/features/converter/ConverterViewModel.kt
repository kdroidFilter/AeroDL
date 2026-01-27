@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ffmpeg.core.AudioOptions
import io.github.kdroidfilter.ffmpeg.core.ConversionOptions
import io.github.kdroidfilter.ffmpeg.core.VideoOptions
import io.github.kdroidfilter.ffmpeg.model.AudioBitrate
import io.github.kdroidfilter.ffmpeg.model.AudioEncoder
import io.github.kdroidfilter.ffmpeg.model.EncoderPreset
import io.github.kdroidfilter.ffmpeg.model.VideoEncoder
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import io.github.kdroidfilter.ytdlpgui.di.AppScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey(ConverterViewModel::class)
@Inject
class ConverterViewModel(
    private val ffmpegWrapper: FfmpegWrapper,
    private val downloadManager: DownloadManager,
    private val trayAppState: TrayAppState
) : MVIViewModel<ConverterState, ConverterEvents>() {

    private var analysisJob: Job? = null

    override fun initialState(): ConverterState = ConverterState()

    override fun handleEvent(event: ConverterEvents) {
        when (event) {
            ConverterEvents.OpenFilePicker -> openFilePicker()
            is ConverterEvents.FileSelected -> analyzeFile(event.file)
            is ConverterEvents.FilesDropped -> {
                update { copy(isDragging = false) }
                event.files.firstOrNull()?.let { analyzeFile(it) }
            }
            ConverterEvents.DragEntered -> update { copy(isDragging = true) }
            ConverterEvents.DragExited -> update { copy(isDragging = false) }
            is ConverterEvents.SetOutputFormat -> update { copy(outputFormat = event.format) }
            is ConverterEvents.SetVideoQuality -> update { copy(selectedVideoQuality = event.quality) }
            is ConverterEvents.SetAudioQuality -> update { copy(selectedAudioQuality = event.quality) }
            ConverterEvents.StartConversion -> startConversion()
            ConverterEvents.CancelConversion -> cancelConversion()
            ConverterEvents.OpenOutputFolder -> openOutputFolder()
            ConverterEvents.Reset -> reset()
            ConverterEvents.ClearError -> update { copy(errorMessage = null) }
        }
    }

    private fun openFilePicker() {
        viewModelScope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(
                    extensions = listOf(
                        "mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v",
                        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus"
                    )
                )
            )
            file?.path?.let { path ->
                analyzeFile(File(path))
            }
        }
    }

    private fun analyzeFile(file: File) {
        // Cancel any ongoing analysis
        analysisJob?.cancel()

        update {
            copy(
                isAnalyzing = true,
                errorMessage = null,
                selectedFile = file,
                mediaInfo = null,
                mediaType = MediaType.UNKNOWN,
                conversionCompleted = false,
                outputFile = null,
                conversionProgress = null
            )
        }

        analysisJob = viewModelScope.launch {
            ffmpegWrapper.analyze(file).fold(
                onSuccess = { mediaInfo ->
                    // Filter out cover art/artwork streams (mjpeg, png, etc.) - they're not real video
                    val coverArtCodecs = setOf("mjpeg", "png", "gif", "bmp", "webp", "tiff")
                    val realVideoStreams = mediaInfo.videoStreams.filter { stream ->
                        stream.codec?.lowercase() !in coverArtCodecs
                    }

                    val mediaType = when {
                        realVideoStreams.isNotEmpty() -> MediaType.VIDEO
                        mediaInfo.audioStreams.isNotEmpty() -> MediaType.AUDIO
                        else -> MediaType.UNKNOWN
                    }

                    val defaultFormat = when (mediaType) {
                        MediaType.VIDEO -> OutputFormat.VIDEO_MP4
                        MediaType.AUDIO -> OutputFormat.AUDIO_MP3
                        MediaType.UNKNOWN -> OutputFormat.VIDEO_MP4
                    }

                    update {
                        copy(
                            isAnalyzing = false,
                            mediaInfo = mediaInfo,
                            mediaType = mediaType,
                            outputFormat = defaultFormat,
                            errorMessage = if (mediaType == MediaType.UNKNOWN) "Unsupported media format" else null
                        )
                    }
                },
                onFailure = { error ->
                    update {
                        copy(
                            isAnalyzing = false,
                            errorMessage = error.message ?: "Failed to analyze file"
                        )
                    }
                }
            )
        }
    }

    private fun startConversion() {
        val currentState = uiState.value
        val inputFile = currentState.selectedFile ?: return
        val mediaInfo = currentState.mediaInfo ?: return

        val outputFile = generateOutputFile(inputFile, currentState.outputFormat)
        val options = buildConversionOptions(currentState)

        // Start conversion via DownloadManager (appears in Tasks screen)
        downloadManager.startConversion(
            inputFile = inputFile,
            outputFile = outputFile,
            options = options,
            totalDuration = mediaInfo.duration
        )

        // Signal navigation to Tasks screen
        update { copy(navigateToTasks = true) }
    }

    fun onNavigationHandled() {
        // Only clear the navigation flag, don't reset the full state
        // The screen is popped from the stack anyway (inclusive = true)
        update { copy(navigateToTasks = false) }
    }

    private fun buildConversionOptions(state: ConverterState): ConversionOptions {
        return when (state.outputFormat) {
            OutputFormat.VIDEO_MP4 -> {
                val videoQuality = state.selectedVideoQuality
                // Use higher CRF values for smaller files while maintaining good quality
                // CRF 28 is a good balance between quality and file size
                // Lower resolutions can use even higher CRF since compression artifacts are less visible
                val crf = when (videoQuality) {
                    VideoQuality.ORIGINAL -> 26  // Good quality, reasonable size
                    VideoQuality.P1080 -> 26
                    VideoQuality.P720 -> 28
                    VideoQuality.P480 -> 30
                    VideoQuality.P360 -> 32
                }
                ConversionOptions(
                    video = VideoOptions(
                        encoder = VideoEncoder.H264,
                        crf = crf,
                        resolution = videoQuality.resolution,
                        preset = EncoderPreset.MEDIUM  // Good compression efficiency
                    ),
                    audio = AudioOptions(
                        encoder = AudioEncoder.AAC,
                        bitrate = AudioBitrate.K128  // 128kbps is sufficient for most use cases
                    )
                )
            }
            OutputFormat.AUDIO_MP3 -> {
                ConversionOptions.audioMp3(state.selectedAudioQuality.bitrate)
            }
        }
    }

    private fun generateOutputFile(inputFile: File, outputFormat: OutputFormat): File {
        val parentDir = inputFile.parentFile ?: File(".")
        val baseName = inputFile.nameWithoutExtension
        val extension = outputFormat.extension

        var outputFile = File(parentDir, "${baseName}_converted.$extension")
        var counter = 1

        // Find a unique filename if it already exists
        while (outputFile.exists()) {
            outputFile = File(parentDir, "${baseName}_converted_$counter.$extension")
            counter++
        }

        return outputFile
    }

    private fun cancelConversion() {
        // Conversions are now managed by DownloadManager
        // User can cancel from the Tasks screen
        update {
            copy(
                isConverting = false,
                conversionProgress = null
            )
        }
    }

    private fun openOutputFolder() {
        val outputFile = uiState.value.outputFile ?: return
        FileExplorerUtils.openDirectoryForPath(outputFile.absolutePath)
    }

    private fun reset() {
        analysisJob?.cancel()
        update { ConverterState() }
    }

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
    }

    fun onScreenEntered() {
        trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
    }

    fun onScreenExited() {
        trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
    }
}
