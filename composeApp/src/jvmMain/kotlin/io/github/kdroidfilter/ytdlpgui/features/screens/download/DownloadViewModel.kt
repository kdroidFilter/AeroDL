package io.github.kdroidfilter.ytdlpgui.features.screens.download

import androidx.lifecycle.ViewModel
import io.github.kdroidfilter.platformtools.OperatingSystem
import io.github.kdroidfilter.platformtools.getOperatingSystem
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import io.github.kdroidfilter.ytdlpgui.data.DownloadHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.awt.Desktop
import java.io.File

class DownloadViewModel(
    private val navigator: Navigator,
    private val downloadManager: DownloadManager,
    private val historyRepository: DownloadHistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val items: StateFlow<List<DownloadManager.DownloadItem>> = downloadManager.items
    val history: StateFlow<List<DownloadHistoryRepository.HistoryItem>> = historyRepository.history

    fun onEvents(event: DownloadEvents) {
        when (event) {
            is DownloadEvents.Cancel -> downloadManager.cancel(event.id)
            DownloadEvents.Refresh -> { /* no-op for now */
            }

            DownloadEvents.ClearHistory -> historyRepository.clear()
            is DownloadEvents.DeleteHistory -> historyRepository.delete(event.id)
            is DownloadEvents.OpenDirectory -> openDirectoryFor(event.id)
        }
    }

    private fun openDirectoryFor(historyId: String) {
        val item = historyRepository.history.value.firstOrNull { it.id == historyId } ?: return
        val path = item.outputPath ?: return
        val target = File(path)

        // Decide if we should try to select a file or just open a directory
        val looksLikeFilePath = target.isFile || (!target.isDirectory && target.name.contains('.'))
        val fileToSelect: File? = when {
            target.isFile -> target
            looksLikeFilePath -> target // may not exist yet, but try to select; fall back to parent dir
            else -> null
        }
        val dirToOpen: File? = when {
            target.isDirectory -> target
            fileToSelect != null -> target.parentFile
            else -> target.parentFile
        }

        fun runCommand(vararg cmd: String): Boolean = try {
            ProcessBuilder(*cmd).start()
            true
        } catch (_: Throwable) {
            false
        }

        try {
            val os = System.getProperty("os.name").lowercase()
            var handled = false

            if (fileToSelect != null) {
                val abs = fileToSelect.absolutePath
                handled = when (getOperatingSystem()) {
                    OperatingSystem.MACOS -> runCommand("open", "-R", abs)
                    OperatingSystem.WINDOWS -> runCommand("cmd", "/c", "explorer /select,\"$abs\"")
                    else -> {

                        // Linux: try common file managers with selection support; fall back to xdg-open on the directory
                        val linuxAttempts: List<Array<String>> = listOf(
                            arrayOf("nautilus", "--select", abs),
                            arrayOf("dolphin", "--select", abs),
                            arrayOf("nemo", "--select", abs),
                            arrayOf("thunar", abs),
                            arrayOf("pcmanfm", abs)
                        )
                        var ok = false
                        for (attempt in linuxAttempts) {
                            if (runCommand(*attempt)) {
                                ok = true; break
                            }
                        }
                        if (!ok) {
                            val dir = dirToOpen
                            if (dir != null) {
                                ok = runCommand("xdg-open", dir.absolutePath)
                            }
                        }
                        ok
                    }
                }
            }

            if (!handled) {
                val dir = dirToOpen
                if (dir != null && dir.exists()) {
                    // Fallback: open the directory using Desktop or xdg-open
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().open(dir); handled = true
                        } catch (_: Throwable) { /* ignore */
                        }
                    }
                    if (!handled) {
                        handled = when (getOperatingSystem()) {
                            OperatingSystem.WINDOWS -> runCommand("explorer.exe", dir.absolutePath)
                            OperatingSystem.MACOS -> runCommand("open", dir.absolutePath)
                            else -> runCommand("xdg-open", dir.absolutePath)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // ignore any OS-level failures entirely
        }
    }
}
