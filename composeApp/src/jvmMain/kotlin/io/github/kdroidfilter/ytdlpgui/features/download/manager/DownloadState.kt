package io.github.kdroidfilter.ytdlpgui.features.download.manager
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.ResolutionAvailability
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import java.time.Duration

data class DownloadState(
    val isLoading: Boolean = false,
    val items: List<DownloadManager.DownloadItem> = emptyList(),
    val history: List<DownloadHistoryRepository.HistoryItem> = emptyList(),
    val directoryAvailability: Map<String, Boolean> = emptyMap(),
    val errorDialogItem: DownloadManager.DownloadItem? = null,
    // App update info (sourced from InitViewModel)
    val updateAvailable: Boolean = false,
    val updateVersion: String? = null,
    val updateUrl: String? = null,
    val updateBody: String? = null,
) {
    companion object {
        val emptyState = DownloadState()

        val withInProgressState = DownloadState(
            items = listOf(
                DownloadManager.DownloadItem(
                    id = "download-1",
                    url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    videoInfo = VideoInfo(
                        id = "dQw4w9WgXcQ",
                        title = "Sample Video Being Downloaded",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        thumbnail = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                        duration = Duration.ofSeconds(212)
                    ),
                    preset = YtDlpWrapper.Preset.P1080,
                    progress = 45.5f,
                    status = DownloadManager.DownloadItem.Status.Running
                ),
                DownloadManager.DownloadItem(
                    id = "download-2",
                    url = "https://www.youtube.com/watch?v=abc123",
                    videoInfo = VideoInfo(
                        id = "abc123",
                        title = "Another Video Pending",
                        url = "https://www.youtube.com/watch?v=abc123",
                        thumbnail = "https://i.ytimg.com/vi/abc123/maxresdefault.jpg",
                        duration = Duration.ofSeconds(180)
                    ),
                    preset = YtDlpWrapper.Preset.P720,
                    progress = 0f,
                    status = DownloadManager.DownloadItem.Status.Pending
                )
            )
        )

        val withHistoryState = DownloadState(
            history = listOf(
                DownloadHistoryRepository.HistoryItem(
                    id = "history-1",
                    url = "https://www.youtube.com/watch?v=completed1",
                    videoInfo = VideoInfo(
                        id = "completed1",
                        title = "Completed Video Download - High Quality",
                        url = "https://www.youtube.com/watch?v=completed1",
                        thumbnail = "https://i.ytimg.com/vi/completed1/maxresdefault.jpg",
                        duration = Duration.ofSeconds(300)
                    ),
                    outputPath = "/home/user/Downloads/video1.mp4",
                    isAudio = false,
                    presetHeight = 1080,
                    createdAt = System.currentTimeMillis() - 3600000 // 1 hour ago
                ),
                DownloadHistoryRepository.HistoryItem(
                    id = "history-2",
                    url = "https://www.youtube.com/watch?v=completed2",
                    videoInfo = VideoInfo(
                        id = "completed2",
                        title = "Audio Only Download",
                        url = "https://www.youtube.com/watch?v=completed2",
                        thumbnail = "https://i.ytimg.com/vi/completed2/maxresdefault.jpg",
                        duration = Duration.ofSeconds(240)
                    ),
                    outputPath = "/home/user/Downloads/audio1.mp3",
                    isAudio = true,
                    presetHeight = null,
                    createdAt = System.currentTimeMillis() - 7200000 // 2 hours ago
                ),
                DownloadHistoryRepository.HistoryItem(
                    id = "history-3",
                    url = "https://www.youtube.com/watch?v=completed3",
                    videoInfo = VideoInfo(
                        id = "completed3",
                        title = "Old Download with Unavailable Directory",
                        url = "https://www.youtube.com/watch?v=completed3",
                        thumbnail = "https://i.ytimg.com/vi/completed3/maxresdefault.jpg",
                        duration = Duration.ofSeconds(150)
                    ),
                    outputPath = "/old/path/video.mp4",
                    isAudio = false,
                    presetHeight = 720,
                    createdAt = System.currentTimeMillis() - 86400000 // 1 day ago
                )
            ),
            directoryAvailability = mapOf(
                "history-1" to true,
                "history-2" to true,
                "history-3" to false
            )
        )

        val mixedState = DownloadState(
            items = listOf(
                DownloadManager.DownloadItem(
                    id = "download-active",
                    url = "https://www.youtube.com/watch?v=active1",
                    videoInfo = VideoInfo(
                        id = "active1",
                        title = "Currently Downloading Video",
                        url = "https://www.youtube.com/watch?v=active1",
                        thumbnail = "https://i.ytimg.com/vi/active1/maxresdefault.jpg",
                        duration = Duration.ofSeconds(420)
                    ),
                    preset = YtDlpWrapper.Preset.P1440,
                    progress = 67.3f,
                    status = DownloadManager.DownloadItem.Status.Running
                )
            ),
            history = listOf(
                DownloadHistoryRepository.HistoryItem(
                    id = "history-recent",
                    url = "https://www.youtube.com/watch?v=recent1",
                    videoInfo = VideoInfo(
                        id = "recent1",
                        title = "Recently Completed Download",
                        url = "https://www.youtube.com/watch?v=recent1",
                        thumbnail = "https://i.ytimg.com/vi/recent1/maxresdefault.jpg",
                        duration = Duration.ofSeconds(360)
                    ),
                    outputPath = "/home/user/Downloads/recent.mp4",
                    isAudio = false,
                    presetHeight = 2160,
                    createdAt = System.currentTimeMillis() - 1800000 // 30 minutes ago
                )
            ),
            directoryAvailability = mapOf("history-recent" to true)
        )

        val withErrorState = DownloadState(
            items = listOf(
                DownloadManager.DownloadItem(
                    id = "download-failed",
                    url = "https://www.youtube.com/watch?v=error1",
                    videoInfo = VideoInfo(
                        id = "error1",
                        title = "Failed Download - Subtitle Error",
                        url = "https://www.youtube.com/watch?v=error1",
                        thumbnail = "https://i.ytimg.com/vi/error1/maxresdefault.jpg",
                        duration = Duration.ofSeconds(350)
                    ),
                    preset = YtDlpWrapper.Preset.P720,
                    progress = 15.0f,
                    status = DownloadManager.DownloadItem.Status.Failed,
                    message = "yt-dlp failed (exit 1). ERROR: Requested subtitles not available for this video.\n--- Last output ---\n[youtube] Extracting URL: https://www.youtube.com/watch?v=error1\n[youtube] error1: Downloading webpage\n[youtube] error1: Downloading ios player API JSON\n[youtube] error1: Downloading m3u8 information\nERROR: Requested subtitles not available for this video"
                ),
                DownloadManager.DownloadItem(
                    id = "download-running",
                    url = "https://www.youtube.com/watch?v=active2",
                    videoInfo = VideoInfo(
                        id = "active2",
                        title = "Another Video Downloading",
                        url = "https://www.youtube.com/watch?v=active2",
                        thumbnail = "https://i.ytimg.com/vi/active2/maxresdefault.jpg",
                        duration = Duration.ofSeconds(280)
                    ),
                    preset = YtDlpWrapper.Preset.P1080,
                    progress = 52.8f,
                    status = DownloadManager.DownloadItem.Status.Running
                )
            )
        )

        val withErrorDialogState = DownloadState(
            items = listOf(
                DownloadManager.DownloadItem(
                    id = "download-failed-with-dialog",
                    url = "https://www.youtube.com/watch?v=error2",
                    videoInfo = VideoInfo(
                        id = "error2",
                        title = "Network Error Download",
                        url = "https://www.youtube.com/watch?v=error2",
                        thumbnail = "https://i.ytimg.com/vi/error2/maxresdefault.jpg",
                        duration = Duration.ofSeconds(195)
                    ),
                    preset = YtDlpWrapper.Preset.P1080,
                    progress = 0f,
                    status = DownloadManager.DownloadItem.Status.Failed,
                    message = "Network pre-check failed.\n--- Last output ---\nConnection timeout after 5000ms\nCould not connect to remote server"
                )
            ),
            errorDialogItem = DownloadManager.DownloadItem(
                id = "download-failed-with-dialog",
                url = "https://www.youtube.com/watch?v=error2",
                videoInfo = VideoInfo(
                    id = "error2",
                    title = "Network Error Download",
                    url = "https://www.youtube.com/watch?v=error2",
                    thumbnail = "https://i.ytimg.com/vi/error2/maxresdefault.jpg",
                    duration = Duration.ofSeconds(195)
                ),
                preset = YtDlpWrapper.Preset.P1080,
                progress = 0f,
                status = DownloadManager.DownloadItem.Status.Failed,
                message = "Network pre-check failed.\n--- Last output ---\nConnection timeout after 5000ms\nCould not connect to remote server"
            )
        )

        val withUpdateInfoState = DownloadState(
            updateAvailable = true,
            updateVersion = "1.2.3",
            updateUrl = "https://example.com/download",
            updateBody = """
                ## Nouveautés
                - Améliorations de performance
                - Corrections de bugs
                Consultez le changelog complet [ici](https://example.com/changelog).
            """.trimIndent(),
            history = withHistoryState.history,
            directoryAvailability = withHistoryState.directoryAvailability,
        )
    }
}
