package io.github.kdroidfilter.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.security.cert.CertPathValidatorException
import java.security.cert.CertPathValidatorException.BasicReason
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import javax.net.ssl.*

/**
 * TLS certificate validator that uses the JVM default trust store (NOT the OS store).
 * Performs a raw TLS handshake (no HTTP) with hostname verification enabled.
 */
object CertificateValidator {

    /** Quick check for YouTube. Mirrors original semantics for non-SSL errors unless strictNetwork=true. */
    suspend fun isYouTubeCertificateValid(
        timeoutMillis: Int = 5_000,
        strictNetwork: Boolean = false
    ): Boolean = checkHost("www.youtube.com", timeoutMillis).let { r ->
        when {
            r.ok -> true
            r.isNetworkIssue && !strictNetwork -> true
            else -> false
        }
    }

    /** Detailed check for any host: returns a structured result with reason and cause chain. */
    suspend fun checkHost(
        host: String,
        timeoutMillis: Int = 5_000,
        port: Int = 443
    ): Result = withContext(Dispatchers.IO) {
        try {
            // Build SSLContext from JVM default trust managers (null => default JVM cacerts)
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as KeyStore?)
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, tmf.trustManagers, null)

            val socket = (ctx.socketFactory.createSocket() as SSLSocket).apply {
                soTimeout = timeoutMillis
                sslParameters = sslParameters.apply {
                    // Enable HTTPS endpoint identification (hostname verification)
                    endpointIdentificationAlgorithm = "HTTPS"
                }
                connect(InetSocketAddress(host, port), timeoutMillis)
                startHandshake()
            }
            socket.close()

            Result(ok = true)
        } catch (t: Throwable) {
            classify(t)
        }
    }

    // ---------------- internals ----------------

    private fun classify(t: Throwable): Result {
        val chain = buildCauseChain(t)

        // Map SSL failures first (these mean the certificate/hostname is not acceptable)
        when (t) {
            is SSLHandshakeException -> {
                // Try to refine using public exceptions only
                findCause<CertificateExpiredException>(t)?.let {
                    return Result(false, FailureReason.CERT_EXPIRED, t.message.orEmpty(), chain)
                }
                findCause<CertificateNotYetValidException>(t)?.let {
                    return Result(false, FailureReason.CERT_NOT_YET_VALID, t.message.orEmpty(), chain)
                }
                findCause<SSLPeerUnverifiedException>(t)?.let {
                    return Result(false, FailureReason.HOSTNAME_MISMATCH, t.message.orEmpty(), chain)
                }
                findCause<CertPathValidatorException>(t)?.let { cpve ->
                    val mapped = when (cpve.reason) {
                        BasicReason.EXPIRED -> FailureReason.CERT_EXPIRED
                        BasicReason.NOT_YET_VALID -> FailureReason.CERT_NOT_YET_VALID
                        BasicReason.REVOKED -> FailureReason.CERT_REVOKED
                        BasicReason.INVALID_SIGNATURE -> FailureReason.CERT_INVALID_SIGNATURE
                        BasicReason.ALGORITHM_CONSTRAINED -> FailureReason.CERT_ALGORITHM_CONSTRAINED
                        else -> FailureReason.CERT_UNTRUSTED // includes "PKIX path building failed"
                    }
                    return Result(false, mapped, cpve.message.orEmpty(), chain)
                }
                return Result(false, FailureReason.SSL_HANDSHAKE, t.message.orEmpty(), chain)
            }
            is SSLPeerUnverifiedException ->
                return Result(false, FailureReason.HOSTNAME_MISMATCH, t.message.orEmpty(), chain)
            is SSLProtocolException ->
                return Result(false, FailureReason.SSL_PROTOCOL, t.message.orEmpty(), chain)
            is SSLException ->
                return Result(false, FailureReason.SSL_GENERIC, t.message.orEmpty(), chain)
        }

        // Network/IO (don’t necessarily imply a bad cert)
        return when (t) {
            is SocketTimeoutException ->
                Result(false, FailureReason.NETWORK_TIMEOUT, t.message.orEmpty(), chain)
            is IOException ->
                Result(false, FailureReason.NETWORK_IO, t.message.orEmpty(), chain)
            else ->
                Result(false, FailureReason.UNKNOWN, t.message.orEmpty(), chain)
        }
    }

    private fun buildCauseChain(t: Throwable): List<String> {
        val chain = mutableListOf<String>()
        var cur: Throwable? = t
        while (cur != null && cur.cause != cur) {
            chain += "${cur::class.simpleName}: ${cur.message.orEmpty()}"
            cur = cur.cause
        }
        return chain
    }

    private inline fun <reified T : Throwable> findCause(t: Throwable): T? {
        var c: Throwable? = t
        while (c != null && c.cause != c) {
            if (c is T) return c
            c = c.cause
        }
        return null
    }

    // --------------- API ---------------

    data class Result(
        val ok: Boolean,
        val reason: FailureReason? = null,
        val message: String = "",
        val causeChain: List<String> = emptyList()
    ) {
        /** True when failure was clearly network-related, not SSL. */
        val isNetworkIssue: Boolean
            get() = reason == FailureReason.NETWORK_TIMEOUT || reason == FailureReason.NETWORK_IO
    }

    enum class FailureReason {
        // SSL-specific
        SSL_HANDSHAKE,
        SSL_PROTOCOL,
        SSL_GENERIC,
        HOSTNAME_MISMATCH,
        CERT_EXPIRED,
        CERT_NOT_YET_VALID,
        CERT_REVOKED,
        CERT_INVALID_SIGNATURE,
        CERT_ALGORITHM_CONSTRAINED,
        CERT_UNTRUSTED,

        // Non-SSL
        NETWORK_TIMEOUT,
        NETWORK_IO,

        // Fallback
        UNKNOWN
    }
}
