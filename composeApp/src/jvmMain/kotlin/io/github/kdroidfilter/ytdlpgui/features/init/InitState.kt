package io.github.kdroidfilter.ytdlpgui.features.init

data class InitState(
    val checkingYtDlp: Boolean = false,
    val checkingFFmpeg: Boolean = false,

    val downloadingYtDlp: Boolean = false,
    val downloadYtDlpProgress: Float? = null,

    val downloadingFFmpeg: Boolean = false,
    val downloadFfmpegProgress: Float? = null,

    val errorMessage: String? = null,

    val updatingYtdlp: Boolean = false,
    val updatingFFmpeg: Boolean = false,

    val initCompleted: Boolean = false,

    // Update check
    val updateAvailable: Boolean = false,
    val latestVersion: String? = null,
    val downloadUrl: String? = null,
) {
    companion object {
        // Preview States
        val checkingYtDlpState = InitState(
            checkingYtDlp = true
        )

        val downloadingYtDlpState = InitState(
            downloadingYtDlp = true,
            downloadYtDlpProgress = 45.5f
        )

        val updatingYtDlpState = InitState(
            updatingYtdlp = true
        )

        val checkingFFmpegState = InitState(
            checkingFFmpeg = true
        )

        val downloadingFFmpegState = InitState(
            downloadingFFmpeg = true,
            downloadFfmpegProgress = 72.3f
        )

        val updatingFFmpegState = InitState(
            updatingFFmpeg = true
        )

        val errorState = InitState(
            errorMessage = "Failed to download yt-dlp: Connection timeout"
        )

        val completedState = InitState(
            initCompleted = true
        )

        val downloadingBothState = InitState(
            downloadingYtDlp = true,
            downloadYtDlpProgress = 35.0f,
            downloadingFFmpeg = true,
            downloadFfmpegProgress = 60.0f
        )
    }
}
