package io.github.kdroidfilter.ytdlpgui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Toolkit.getDefaultToolkit
import org.jetbrains.compose.resources.getString
import ytdlpgui.composeapp.generated.resources.*
import java.awt.datatransfer.DataFlavor
import java.net.URI

class HomeViewModel(
    private val navController: NavHostController,
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
                // Validate as the user types
                validateLink(event.link)
            }
            HomeEvents.OnNextClicked -> checkLink()
            HomeEvents.OnClipBoardClicked -> copyFromClipboard()
        }
    }

    private fun copyFromClipboard() {
        val clipboard = getDefaultToolkit().systemClipboard
        val contents = clipboard.getContents(null)
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                val pasted = contents.getTransferData(DataFlavor.stringFlavor) as String
                _textFieldContent.value = pasted
                // Validate immediately after pasting
                validateLink(pasted)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validateLink(inputRaw: String): Boolean {
        val input = inputRaw.trim()
        // If empty, clear error and consider invalid (can't navigate yet)
        if (input.isEmpty()) {
            _errorMessage.value = null
            return false
        }

        // Extract a single URL from the input using a simple regex for http/https links
        val urlRegex = Regex("""https?://\S+""")
        val matches = urlRegex.findAll(input).toList()

        if (matches.size != 1 || matches.first().value != input) {
            // Either multiple URLs or extra text around the URL
            viewModelScope.launch {
                _errorMessage.value = getString(Res.string.error_single_valid_url)
            }
            return false
        }

        val url = matches.first().value
        // Validate URL structure
        return try {
            URI(url)
            // Looks valid
            _errorMessage.value = null
            true
        } catch (e: Exception) {
            viewModelScope.launch {
                _errorMessage.value = getString(Res.string.error_invalid_url_format)
            }
            false
        }
    }

    private fun checkLink() {
        val current = textFieldContent.value
        // If empty, show an explicit error when Next is pressed
        if (current.trim().isEmpty()) {
            viewModelScope.launch {
                _errorMessage.value = getString(Res.string.error_url_required)
            }
            return
        }
        val isValid = validateLink(current)
        if (!isValid) return

        val url = current.trim()

        // Classify: playlist/channel → Bulk, otherwise Single
        val lower = url.lowercase()
        val isYouTube = listOf("youtube.com", "youtu.be").any { lower.contains(it) }
        val isPlaylist = lower.contains("list=") || lower.contains("/playlist")
        val isChannel = lower.contains("/channel/") || lower.contains("/c/") || (isYouTube && lower.contains("youtube.com/@"))

        viewModelScope.launch {
            if (isYouTube && (isPlaylist || isChannel)) {
                navController.navigate(Destination.Download.Bulk(url))
            } else {
                navController.navigate(Destination.Download.Single(url))
            }
        }
    }

}