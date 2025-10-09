package io.github.kdroidfilter.ytdlpgui.core.platform.network

import org.jetbrains.nativecerts.NativeTrustedCertificates
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Provides SSL/TLS configuration that uses native OS certificate stores.
 * This ensures all network calls respect system root certificates.
 */
object TrustedRootsSSL {

    /**
     * Trust manager that uses OS native trusted roots
     */
    val trustManager: X509TrustManager by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            NativeTrustedCertificates.getCustomOsSpecificTrustedCertificates().forEach { cert ->
                setCertificateEntry(cert.subjectX500Principal.name, cert)
            }
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /**
     * SSLContext configured with native trusted roots
     */
    val sslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
    }

    /**
     * SSLSocketFactory configured with native trusted roots
     */
    val socketFactory by lazy {
        sslContext.socketFactory
    }
}