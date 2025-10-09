package io.github.kdroidfilter.ytdlp.core.platform

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json as KotlinxJson

/**
 * Provides a configured Ktor HttpClient that uses native OS certificate stores.
 */
object KtorConfig {

    /**
     * Creates a Ktor HttpClient configured with native trusted roots
     */
    fun createHttpClient(): HttpClient = HttpClient(CIO) {
        engine {
            https {
                trustManager = TrustedRootsSSL.trustManager
            }
        }

        install(ContentNegotiation) {
            json(KotlinxJson {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = LogLevel.INFO
        }
    }
}