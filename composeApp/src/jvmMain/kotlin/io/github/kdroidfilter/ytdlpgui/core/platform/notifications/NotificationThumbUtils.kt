package io.github.kdroidfilter.ytdlpgui.core.platform.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.github.kdroidfilter.ytdlp.util.YouTubeThumbnailHelper
import io.github.kdroidfilter.ytdlpgui.core.platform.network.TrustedRootsSSL
import org.jetbrains.skia.Image as SkiaImage
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object NotificationThumbUtils {
    /**
     * Resolve a thumbnail URL to use in notifications. Prefers a provided direct thumbnail,
     * otherwise tries to derive a YouTube thumbnail from the URL if possible.
     */
    fun resolveThumbnailUrl(directThumb: String?, url: String): String? {
        if (!directThumb.isNullOrBlank()) return directThumb
        val id = YouTubeThumbnailHelper.extractVideoId(url) ?: return null
        return YouTubeThumbnailHelper.getThumbnailUrl(id, YouTubeThumbnailHelper.ThumbnailQuality.MEDIUM)
    }

    /**
     * Build a composable lambda that loads and renders the image at [thumbUrl] as a large icon.
     * Returns null if [thumbUrl] is null.
     */
    fun buildLargeIcon(thumbUrl: String?): (@Composable () -> Unit)? {
        if (thumbUrl.isNullOrBlank()) return null
        return {
            var imageBitmap by remember(thumbUrl) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(thumbUrl) {
                runCatching {
                    val url = URL(thumbUrl)
                    val bytes = if (url.protocol.equals("https", ignoreCase = true)) {
                        val connection = url.openConnection() as HttpsURLConnection
                        connection.sslSocketFactory = TrustedRootsSSL.socketFactory
                        connection.inputStream.readBytes()
                    } else {
                        url.readBytes()
                    }
                    val skiaImage = SkiaImage.makeFromEncoded(bytes)
                    imageBitmap = skiaImage.toComposeImageBitmap()
                }
            }
            imageBitmap?.let { img ->
                Image(
                    bitmap = img,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
