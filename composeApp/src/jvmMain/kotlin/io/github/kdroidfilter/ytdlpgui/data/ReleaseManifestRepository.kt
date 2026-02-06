package io.github.kdroidfilter.ytdlpgui.data

import io.github.kdroidfilter.network.HttpsConnectionFactory
import io.github.kdroidfilter.ytdlp.model.ReleaseManifest
import kotlinx.serialization.json.Json

/**
 * Fetches and caches the static release manifest.
 * Only ONE network call per application session; subsequent calls return the cached result.
 */
class ReleaseManifestRepository {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: ReleaseManifest? = null

    suspend fun getManifest(): ReleaseManifest? {
        cached?.let { return it }
        return runCatching {
            val conn = HttpsConnectionFactory.openConnection(MANIFEST_URL) {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "AeroDl/1.0")
                setRequestProperty("Accept", "application/json")
            }
            val body = conn.inputStream.use { it.bufferedReader().readText() }
            json.decodeFromString<ReleaseManifest>(body)
        }.getOrNull()?.also { cached = it }
    }

    companion object {
        const val MANIFEST_URL = "https://kdroidfilter.github.io/AeroDL/api/releases.json"
    }
}
