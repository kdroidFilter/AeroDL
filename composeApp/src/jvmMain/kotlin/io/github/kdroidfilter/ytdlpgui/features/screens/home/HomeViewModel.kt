package io.github.kdroidfilter.ytdlpgui.features.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.presentation.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Toolkit.getDefaultToolkit

class HomeViewModel(
    private val navigator: Navigator,
    private val ytDlpWrapper: YtDlpWrapper
) : ViewModel() {
    private var _textFieldContent = MutableStateFlow("")
    val textFieldContent = _textFieldContent.asStateFlow()

    private var _textFieldEnabled = MutableStateFlow(false)
    val textFieldEnabled = _textFieldEnabled.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun onEvents(event: HomeEvents) {
        when (event) {
            is HomeEvents.OnLinkChanged -> {
                _textFieldContent.value = event.link
                _errorMessage.value = null
            }
            HomeEvents.OnNextClicked -> checkLink()
            HomeEvents.OnClipBoardClicked -> copyFromClipboard()
        }
    }

    private fun copyFromClipboard() {
        val clipboard = getDefaultToolkit().systemClipboard
        val contents = clipboard.getContents(null)
        if (contents != null && contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
            try {
                _textFieldContent.value =
                    contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as String
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkLink() {
        val input = textFieldContent.value.trim()
        // Extract a single URL from the input using a simple regex for http/https links
        val urlRegex = Regex("""https?://[^\s]+""")
        val matches = urlRegex.findAll(input).toList()

        if (matches.size != 1 || matches.first().value != input) {
            _errorMessage.value = "Please paste a single valid URL."
            return
        }

        val url = matches.first().value
        // Validate URL structure
        try {
            java.net.URL(url).toURI()
        } catch (e: Exception) {
            _errorMessage.value = "Invalid URL format."
            return
        }

        // Classify: playlist/channel → Bulk, otherwise Single
        val lower = url.lowercase()
        val isYouTube = listOf("youtube.com", "youtu.be").any { lower.contains(it) }
        val isPlaylist = lower.contains("list=") || lower.contains("/playlist")
        val isChannel = lower.contains("/channel/") || lower.contains("/c/") || (isYouTube && lower.contains("youtube.com/@"))

        viewModelScope.launch {
            if (isYouTube && (isPlaylist || isChannel)) {
                navigator.navigate(Destination.BulkDownloadScreen(url))
            } else {
                navigator.navigate(Destination.SingleDownloadScreen(url))
            }
        }
    }

}