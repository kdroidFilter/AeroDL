package io.github.kdroidfilter.ytdlpgui.features.onboarding.downloaddir

data class DownloadDirState(
    val downloadDirPath: String = ""
) {
    companion object {
        val emptyState = DownloadDirState()
        val configuredState = DownloadDirState(
            downloadDirPath = "/home/user/Downloads"
        )
    }
}
