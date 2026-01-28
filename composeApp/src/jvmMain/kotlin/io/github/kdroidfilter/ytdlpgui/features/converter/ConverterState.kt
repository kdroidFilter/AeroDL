package io.github.kdroidfilter.ytdlpgui.features.converter

import io.github.kdroidfilter.ffmpeg.model.AudioBitrate
import io.github.kdroidfilter.ffmpeg.model.MediaInfo
import io.github.kdroidfilter.ffmpeg.model.Resolution
import java.io.File

/**
 * Type of media detected from the input file.
 */
enum class MediaType {
    VIDEO,
    AUDIO,
    UNKNOWN
}

/**
 * Output format for the conversion.
 */
enum class OutputFormat(val extension: String, val displayName: String) {
    VIDEO_MP4("mp4", "Video (MP4)"),
    AUDIO_MP3("mp3", "Audio (MP3)")
}

/**
 * Video quality preset for conversion.
 */
enum class VideoQuality(val resolution: Resolution?, val displayName: String) {
    ORIGINAL(null, "Original"),
    P1080(Resolution.P1080, "1080p (Full HD)"),
    P720(Resolution.P720, "720p (HD)"),
    P480(Resolution.P480, "480p (SD)"),
    P360(Resolution.P360, "360p")
}

/**
 * Audio quality preset for conversion.
 */
enum class AudioQuality(val bitrate: AudioBitrate, val displayName: String) {
    HIGH(AudioBitrate.K320, "320 kbps (High)"),
    GOOD(AudioBitrate.K256, "256 kbps"),
    STANDARD(AudioBitrate.K192, "192 kbps (Recommended)"),
    LOW(AudioBitrate.K128, "128 kbps")
}

/**
 * UI state for the Converter screen.
 */
data class ConverterState(
    val isAnalyzing: Boolean = false,
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
    val isDragging: Boolean = false,
    val navigateToTasks: Boolean = false
) {
    val canConvert: Boolean
        get() = selectedFile != null && mediaInfo != null && !isConverting && !conversionCompleted

    val showVideoOptions: Boolean
        get() = mediaType == MediaType.VIDEO && outputFormat == OutputFormat.VIDEO_MP4

    val showAudioOptions: Boolean
        get() = outputFormat == OutputFormat.AUDIO_MP3

    val showFormatSelector: Boolean
        get() = mediaType == MediaType.VIDEO
}
