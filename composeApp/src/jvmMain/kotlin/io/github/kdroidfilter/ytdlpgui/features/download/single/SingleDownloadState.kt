package io.github.kdroidfilter.ytdlpgui.features.download.single

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.ResolutionAvailability
import io.github.kdroidfilter.ytdlp.model.SubtitleInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import java.time.Duration

data class SingleDownloadState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val videoInfo: VideoInfo? = null,
    val availablePresets: List<YtDlpWrapper.Preset> = emptyList(),
    val selectedPreset: YtDlpWrapper.Preset? = null,
    val availableSubtitleLanguages: List<String> = emptyList(),
    val selectedSubtitles: List<String> = emptyList(),
) {
    companion object {
        val loadingState = SingleDownloadState(
            isLoading = true
        )

        val emptyState = SingleDownloadState()

        val errorState = SingleDownloadState(
            errorMessage = "Failed to load video information"
        )

        val loadedState = SingleDownloadState(
            videoInfo = VideoInfo(
                id = "dQw4w9WgXcQ",
                title = "Sample Video Title - This is a longer title to demonstrate text overflow behavior",
                url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                thumbnail = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                description = "This is a sample video description that demonstrates how the UI handles longer text content. It should show proper text wrapping and overflow behavior.",
                uploader = "Sample Channel",
                duration = Duration.ofSeconds(212),
                viewCount = 1234567890,
                likeCount = 12345678,
                uploadDate = "2023-12-01",
                availableResolutions = mapOf(
                    2160 to ResolutionAvailability(progressive = true, downloadable = true),
                    1440 to ResolutionAvailability(progressive = true, downloadable = true),
                    1080 to ResolutionAvailability(progressive = true, downloadable = true),
                    720 to ResolutionAvailability(progressive = true, downloadable = true),
                    480 to ResolutionAvailability(progressive = true, downloadable = true),
                    360 to ResolutionAvailability(progressive = true, downloadable = true)
                ),
                directUrl = "https://example.com/video.mp4",
                availableSubtitles = mapOf(
                    "en" to SubtitleInfo(language = "en", languageName = "English"),
                    "fr" to SubtitleInfo(language = "fr", languageName = "French"),
                    "es" to SubtitleInfo(language = "es", languageName = "Spanish")
                )
            ),
            availablePresets = listOf(
                YtDlpWrapper.Preset.P2160,
                YtDlpWrapper.Preset.P1440,
                YtDlpWrapper.Preset.P1080,
                YtDlpWrapper.Preset.P720,
                YtDlpWrapper.Preset.P480,
                YtDlpWrapper.Preset.P360
            ),
            selectedPreset = YtDlpWrapper.Preset.P1080,
            availableSubtitleLanguages = listOf("en", "fr", "es", "de", "ja"),
            selectedSubtitles = listOf("en", "fr")
        )

        val withSubtitlesState = SingleDownloadState(
            videoInfo = VideoInfo(
                id = "abc123",
                title = "Tutorial Video with Multiple Subtitles",
                url = "https://www.youtube.com/watch?v=abc123",
                thumbnail = "https://i.ytimg.com/vi/abc123/maxresdefault.jpg",
                description = "A tutorial video with multiple subtitle tracks available.",
                uploader = "Tutorial Channel",
                duration = Duration.ofSeconds(600),
                viewCount = 500000,
                likeCount = 25000,
                uploadDate = "2024-01-15",
                availableResolutions = mapOf(
                    1080 to ResolutionAvailability(progressive = true, downloadable = true),
                    720 to ResolutionAvailability(progressive = true, downloadable = true)
                ),
                directUrl = "https://example.com/tutorial.mp4",
                availableSubtitles = mapOf(
                    "en" to SubtitleInfo(language = "en", languageName = "English"),
                    "fr" to SubtitleInfo(language = "fr", languageName = "French"),
                    "es" to SubtitleInfo(language = "es", languageName = "Spanish"),
                    "de" to SubtitleInfo(language = "de", languageName = "German"),
                    "ja" to SubtitleInfo(language = "ja", languageName = "Japanese"),
                    "ko" to SubtitleInfo(language = "ko", languageName = "Korean"),
                    "zh" to SubtitleInfo(language = "zh", languageName = "Chinese")
                )
            ),
            availablePresets = listOf(
                YtDlpWrapper.Preset.P1080,
                YtDlpWrapper.Preset.P720
            ),
            selectedPreset = YtDlpWrapper.Preset.P720,
            availableSubtitleLanguages = listOf("en", "fr", "es", "de", "ja", "ko", "zh"),
            selectedSubtitles = listOf("en", "fr", "es")
        )
    }
}

@Composable
fun collectSingleDownloadState(viewModel: SingleDownloadViewModel): SingleDownloadState =
    SingleDownloadState(
        isLoading = viewModel.isLoading.collectAsState().value,
        errorMessage = viewModel.errorMessage.collectAsState().value,
        videoInfo = viewModel.videoInfo.collectAsState().value,
        availablePresets = viewModel.availablePresets.collectAsState().value,
        selectedPreset = viewModel.selectedPreset.collectAsState().value,
        availableSubtitleLanguages = viewModel.availableSubtitleLanguages.collectAsState().value,
        selectedSubtitles = viewModel.selectedSubtitles.collectAsState().value,
    )
