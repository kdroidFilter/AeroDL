package io.github.kdroidfilter.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.github.kdroidfilter.logging.LoggerConfig

/**
 * Provides a configured Ktor HttpClient that uses native OS certificate stores.
 */
object KtorConfig {

    /**
     * Creates a Ktor HttpClient configured with native trusted roots
     *
     * @param logLevel The logging level for HTTP requests/responses (default: INFO)
     * @param json Custom JSON configuration (default: ignoreUnknownKeys + isLenient)
     * @return Configured HttpClient instance
     */
    fun createHttpClient(
        logLevel: LogLevel = if (LoggerConfig.enabled) LogLevel.INFO else LogLevel.NONE,
        json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    ): HttpClient = HttpClient(CIO) {
        engine {
            https {
                trustManager = TrustedRootsSSL.trustManager
            }

            // Configure timeouts
            requestTimeout = 30_000

            // Enable endpoint for proper HTTPS hostname verification
            endpoint {
                connectTimeout = 15_000
                socketTimeout = 30_000
            }
        }

        // Follow redirects (GitHub Pages may redirect)
        followRedirects = true

        // Configure request timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            level = logLevel
        }
    }
}
