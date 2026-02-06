package io.github.kdroidfilter.ytdlpgui.data

import io.github.kdroidfilter.logging.LoggerConfig
import io.github.kdroidfilter.network.KtorConfig
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * Fetches and caches the static release manifest.
 * Only ONE network call per application session; subsequent calls return the cached result.
 */
class ReleaseManifestRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy { KtorConfig.createHttpClient(json = json) }

    @Volatile
    private var cached: ReleaseManifest? = null

    suspend fun getManifest(): ReleaseManifest? {
        cached?.let { return it }

        return runCatching {
            val response: HttpResponse = httpClient.get(MANIFEST_URL) {
                header(HttpHeaders.UserAgent, "AeroDl/1.0")
                header(HttpHeaders.Accept, "application/json")
            }
            val body = response.bodyAsText()
            json.decodeFromString<ReleaseManifest>(body)
        }.onFailure { error ->
            if (LoggerConfig.enabled) {
                System.err.println("[ReleaseManifestRepository] Failed to fetch manifest: ${error.message}")
                error.printStackTrace()
            }
        }.getOrNull()?.also { cached = it }
    }

    companion object {
        const val MANIFEST_URL = "https://kdroidfilter.github.io/AeroDL/api/releases.json"
    }
}
