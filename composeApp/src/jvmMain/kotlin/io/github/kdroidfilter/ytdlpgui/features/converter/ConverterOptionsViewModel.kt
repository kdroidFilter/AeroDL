@file:OptIn(ExperimentalTrayAppApi::class)

package io.github.kdroidfilter.ytdlpgui.features.converter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kdroid.composetray.tray.api.ExperimentalTrayAppApi
import com.kdroid.composetray.tray.api.TrayAppState
import com.kdroid.composetray.tray.api.TrayWindowDismissMode
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.kdroidfilter.ffmpeg.FfmpegWrapper
import io.github.kdroidfilter.ffmpeg.core.AudioOptions
import io.github.kdroidfilter.ffmpeg.core.ConversionOptions
import io.github.kdroidfilter.ffmpeg.core.VideoOptions
import io.github.kdroidfilter.ffmpeg.model.AudioBitrate
import io.github.kdroidfilter.ffmpeg.model.AudioEncoder
import io.github.kdroidfilter.ffmpeg.model.EncoderPreset
import io.github.kdroidfilter.ffmpeg.model.MediaInfo
import io.github.kdroidfilter.ffmpeg.model.VideoEncoder
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.platform.filesystem.FileExplorerUtils
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration

/**
 * Navigation state for ConverterOptionsScreen
 */
sealed interface ConverterOptionsNavigationState {
    data object None : ConverterOptionsNavigationState
    data object NavigateToTasks : ConverterOptionsNavigationState
}

/**
 * Events for the Converter Options screen
 */
sealed class ConverterOptionsEvents {
    data class SetOutputFormat(val format: OutputFormat) : ConverterOptionsEvents()
    data class SetVideoQuality(val quality: VideoQuality) : ConverterOptionsEvents()
    data class SetAudioQuality(val quality: AudioQuality) : ConverterOptionsEvents()
    data class SetTrimRange(val startMs: Long, val endMs: Long) : ConverterOptionsEvents()
    data object StartConversion : ConverterOptionsEvents()
    data object CancelConversion : ConverterOptionsEvents()
    data object OpenOutputFolder : ConverterOptionsEvents()
    data object Reset : ConverterOptionsEvents()
    data object ClearError : ConverterOptionsEvents()
    data object OnNavigationConsumed : ConverterOptionsEvents()
    data object ScreenDisposed : ConverterOptionsEvents()
}

/**
 * UI State for the Converter Options screen
 */
data class ConverterOptionsState(
    val isAnalyzing: Boolean = true,
    val errorMessage: String? = null,
    val selectedFile: File? = null,
    val mediaInfo: MediaInfo? = null,
    val mediaType: MediaType = MediaType.UNKNOWN,
    val outputFormat: OutputFormat = OutputFormat.VIDEO_MP4,
    val selectedVideoQuality: VideoQuality = VideoQuality.ORIGINAL,
    val selectedAudioQuality: AudioQuality = AudioQuality.STANDARD,
    val isConverting: Boolean = false,
    val conversionProgress: Float? = null,
    val conversionCompleted: Boolean = false,
    val outputFile: File? = null,
    val navigationState: ConverterOptionsNavigationState = ConverterOptionsNavigationState.None,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val totalDurationMs: Long = 0L
) {
    val canConvert: Boolean
        get() = selectedFile != null && mediaInfo != null && !isConverting && !conversionCompleted

    val showVideoOptions: Boolean
        get() = mediaType == MediaType.VIDEO && outputFormat == OutputFormat.VIDEO_MP4

    val showAudioOptions: Boolean
        get() = outputFormat == OutputFormat.AUDIO_MP3

    val showFormatSelector: Boolean
        get() = mediaType == MediaType.VIDEO

    val showTrimSlider: Boolean
        get() = totalDurationMs > 0L

    val isTrimmed: Boolean
        get() = trimStartMs > 0L || (trimEndMs > 0L && trimEndMs < totalDurationMs)

    companion object {
        val loadingState = ConverterOptionsState(isAnalyzing = true)
    }
}

class ConverterOptionsViewModel @AssistedInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val ffmpegWrapper: FfmpegWrapper,
    private val downloadManager: DownloadManager,
    private val trayAppState: TrayAppState
) : MVIViewModel<ConverterOptionsState, ConverterOptionsEvents>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): ConverterOptionsViewModel
    }

    private var analysisJob: Job? = null

    val filePath = savedStateHandle.toRoute<Destination.Converter.Options>().filePath

    private val _isAnalyzing = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _selectedFile = MutableStateFlow<File?>(null)
    private val _mediaInfo = MutableStateFlow<MediaInfo?>(null)
    private val _mediaType = MutableStateFlow(MediaType.UNKNOWN)
    private val _outputFormat = MutableStateFlow(OutputFormat.VIDEO_MP4)
    private val _selectedVideoQuality = MutableStateFlow(VideoQuality.ORIGINAL)
    private val _selectedAudioQuality = MutableStateFlow(AudioQuality.STANDARD)
    private val _isConverting = MutableStateFlow(false)
    private val _conversionProgress = MutableStateFlow<Float?>(null)
    private val _conversionCompleted = MutableStateFlow(false)
    private val _outputFile = MutableStateFlow<File?>(null)
    private val _navigationState = MutableStateFlow<ConverterOptionsNavigationState>(ConverterOptionsNavigationState.None)
    private val _trimStartMs = MutableStateFlow(0L)
    private val _trimEndMs = MutableStateFlow(0L)
    private val _totalDurationMs = MutableStateFlow(0L)

    override val uiState = combine(
        _isAnalyzing,
        _errorMessage,
        _selectedFile,
        _mediaInfo,
        _mediaType,
        _outputFormat,
        _selectedVideoQuality,
        _selectedAudioQuality,
        _isConverting,
        _conversionProgress,
        _conversionCompleted,
        _outputFile,
        _navigationState,
        _trimStartMs,
        _trimEndMs,
        _totalDurationMs
    ) { values: Array<Any?> ->
        ConverterOptionsState(
            isAnalyzing = values[0] as Boolean,
            errorMessage = values[1] as String?,
            selectedFile = values[2] as File?,
            mediaInfo = values[3] as MediaInfo?,
            mediaType = values[4] as MediaType,
            outputFormat = values[5] as OutputFormat,
            selectedVideoQuality = values[6] as VideoQuality,
            selectedAudioQuality = values[7] as AudioQuality,
            isConverting = values[8] as Boolean,
            conversionProgress = values[9] as Float?,
            conversionCompleted = values[10] as Boolean,
            outputFile = values[11] as File?,
            navigationState = values[12] as ConverterOptionsNavigationState,
            trimStartMs = values[13] as Long,
            trimEndMs = values[14] as Long,
            totalDurationMs = values[15] as Long
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConverterOptionsState.loadingState
    )

    override fun initialState(): ConverterOptionsState = ConverterOptionsState.loadingState

    init {
        analyzeFile(File(filePath))
        trayAppState.setDismissMode(TrayWindowDismissMode.MANUAL)
    }

    override fun handleEvent(event: ConverterOptionsEvents) {
        when (event) {
            is ConverterOptionsEvents.SetOutputFormat -> _outputFormat.value = event.format
            is ConverterOptionsEvents.SetVideoQuality -> _selectedVideoQuality.value = event.quality
            is ConverterOptionsEvents.SetAudioQuality -> _selectedAudioQuality.value = event.quality
            is ConverterOptionsEvents.SetTrimRange -> {
                _trimStartMs.value = event.startMs
                _trimEndMs.value = event.endMs
            }
            ConverterOptionsEvents.StartConversion -> startConversion()
            ConverterOptionsEvents.CancelConversion -> cancelConversion()
            ConverterOptionsEvents.OpenOutputFolder -> openOutputFolder()
            ConverterOptionsEvents.Reset -> _navigationState.value = ConverterOptionsNavigationState.None
            ConverterOptionsEvents.ClearError -> _errorMessage.value = null
            ConverterOptionsEvents.OnNavigationConsumed -> _navigationState.value = ConverterOptionsNavigationState.None
            ConverterOptionsEvents.ScreenDisposed -> {
                trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
                analysisJob?.cancel()
            }
        }
    }

    private fun analyzeFile(file: File) {
        analysisJob?.cancel()

        _isAnalyzing.value = true
        _errorMessage.value = null
        _selectedFile.value = file
        _mediaInfo.value = null
        _mediaType.value = MediaType.UNKNOWN
        _conversionCompleted.value = false
        _outputFile.value = null
        _conversionProgress.value = null
        _trimStartMs.value = 0L
        _trimEndMs.value = 0L
        _totalDurationMs.value = 0L

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

                    // Initialize trim values based on media duration
                    val durationMs = mediaInfo.duration?.toMillis() ?: 0L
                    _totalDurationMs.value = durationMs
                    _trimStartMs.value = 0L
                    _trimEndMs.value = durationMs

                    _isAnalyzing.value = false
                    _mediaInfo.value = mediaInfo
                    _mediaType.value = mediaType
                    _outputFormat.value = defaultFormat
                    if (mediaType == MediaType.UNKNOWN) {
                        _errorMessage.value = "Unsupported media format"
                    }
                },
                onFailure = { error ->
                    _isAnalyzing.value = false
                    _errorMessage.value = error.message ?: "Failed to analyze file"
                }
            )
        }
    }

    private fun startConversion() {
        val inputFile = _selectedFile.value ?: return
        val mediaInfo = _mediaInfo.value ?: return

        val outputFile = generateOutputFile(inputFile, _outputFormat.value)
        val options = buildConversionOptions()

        // Calculate actual duration for progress (trimmed if applicable)
        val totalMs = _totalDurationMs.value
        val startMs = _trimStartMs.value
        val endMs = _trimEndMs.value
        val isTrimmed = startMs > 0L || (endMs > 0L && endMs < totalMs)
        val effectiveDuration = if (isTrimmed) {
            Duration.ofMillis(endMs - startMs)
        } else {
            mediaInfo.duration
        }

        // Start conversion via DownloadManager (appears in Tasks screen)
        downloadManager.startConversion(
            inputFile = inputFile,
            outputFile = outputFile,
            options = options,
            totalDuration = effectiveDuration
        )

        // Signal navigation to Tasks screen
        _navigationState.value = ConverterOptionsNavigationState.NavigateToTasks
    }

    private fun buildConversionOptions(): ConversionOptions {
        // Check if trim is needed (range differs from full duration)
        val totalMs = _totalDurationMs.value
        val startMs = _trimStartMs.value
        val endMs = _trimEndMs.value
        val isTrimmed = startMs > 0L || (endMs > 0L && endMs < totalMs)

        val startTime = if (isTrimmed && startMs > 0L) Duration.ofMillis(startMs) else null
        val duration = if (isTrimmed && endMs < totalMs) Duration.ofMillis(endMs - startMs) else null

        return when (_outputFormat.value) {
            OutputFormat.VIDEO_MP4 -> {
                val videoQuality = _selectedVideoQuality.value
                val crf = when (videoQuality) {
                    VideoQuality.ORIGINAL -> 26
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
                        preset = EncoderPreset.MEDIUM
                    ),
                    audio = AudioOptions(
                        encoder = AudioEncoder.AAC,
                        bitrate = AudioBitrate.K128
                    ),
                    startTime = startTime,
                    duration = duration
                )
            }
            OutputFormat.AUDIO_MP3 -> {
                ConversionOptions.audioMp3(_selectedAudioQuality.value.bitrate).copy(
                    startTime = startTime,
                    duration = duration
                )
            }
        }
    }

    private fun generateOutputFile(inputFile: File, outputFormat: OutputFormat): File {
        val parentDir = inputFile.parentFile ?: File(".")
        val baseName = inputFile.nameWithoutExtension
        val extension = outputFormat.extension

        var outputFile = File(parentDir, "${baseName}_converted.$extension")
        var counter = 1

        while (outputFile.exists()) {
            outputFile = File(parentDir, "${baseName}_converted_$counter.$extension")
            counter++
        }

        return outputFile
    }

    private fun cancelConversion() {
        _isConverting.value = false
        _conversionProgress.value = null
    }

    private fun openOutputFolder() {
        val outputFile = _outputFile.value ?: return
        FileExplorerUtils.openDirectoryForPath(outputFile.absolutePath)
    }

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
        trayAppState.setDismissMode(TrayWindowDismissMode.AUTO)
    }
}
