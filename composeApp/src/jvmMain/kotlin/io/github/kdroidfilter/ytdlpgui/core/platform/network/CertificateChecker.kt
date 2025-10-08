package io.github.kdroidfilter.ytdlpgui.core.platform.network

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.security.cert.X509Certificate

/**
 * Utility to check SSL certificate information for a given URL
 */
object CertificateChecker {

    /**
     * Checks if the YouTube certificate issuer is Google Trust Services
     * @return true if the certificate is from Google Trust Services, false otherwise
     */
    fun isYouTubeCertificateFromGoogleTrustServices(): Boolean {
        return try {
            val url = URL("https://www.youtube.com")
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val certificates = connection.serverCertificates
            connection.disconnect()

            if (certificates.isNotEmpty()) {
                val cert = certificates[0] as? X509Certificate
                val issuer = cert?.issuerX500Principal?.name ?: ""
                // Check if Organization is Google Trust Services
                issuer.contains("O=Google Trust Services", ignoreCase = true)
            } else {
                false
            }
        } catch (e: Exception) {
            // In case of error, assume certificate check is needed
            false
        }
    }
}