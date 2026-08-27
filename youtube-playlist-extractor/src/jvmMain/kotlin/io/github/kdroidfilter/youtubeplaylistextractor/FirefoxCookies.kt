package io.github.kdroidfilter.youtubeplaylistextractor

import io.github.kdroidfilter.logging.debugln
import io.github.kdroidfilter.logging.warnln
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager

data class Cookie(val name: String, val value: String, val host: String)

object FirefoxCookies {

    fun loadForDomain(domain: String): List<Cookie> {
        val cookiesDb = findCookiesDb()
        if (cookiesDb == null) {
            warnln { "[FirefoxCookies] cookies.sqlite not found" }
            return emptyList()
        }

        val tmp = Files.createTempFile("ff_cookies_", ".sqlite")
        Files.copy(cookiesDb, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING)

        return try {
            DriverManager.getConnection("jdbc:sqlite:${tmp.toAbsolutePath()}").use { conn ->
                conn.prepareStatement(
                    "SELECT name, value, host FROM moz_cookies WHERE host LIKE ?"
                ).use { stmt ->
                    stmt.setString(1, "%$domain")
                    stmt.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    Cookie(
                                        name = rs.getString("name"),
                                        value = rs.getString("value"),
                                        host = rs.getString("host"),
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            warnln { "[FirefoxCookies] Failed to read cookies: ${e.message}" }
            emptyList()
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    fun toHeader(cookies: List<Cookie>): String =
        cookies
            .filter { cookie ->
                cookie.name.isNotBlank() &&
                    cookie.value.none { it == '\n' || it == '\r' || it == ';' }
            }
            .distinctBy { it.name }
            .joinToString("; ") { "${it.name}=${it.value}" }

    private fun findCookiesDb(): Path? {
        val profilesDir = profilesDir() ?: return null
        if (!Files.isDirectory(profilesDir)) return null

        return Files.list(profilesDir).use { stream ->
            stream
                .filter { Files.isDirectory(it) }
                .map { it.resolve("cookies.sqlite") }
                .filter { Files.exists(it) }
                .max(Comparator.comparingLong { Files.getLastModifiedTime(it).toMillis() })
                .orElse(null)
        }
    }

    private fun profilesDir(): Path? {
        val home = Path.of(System.getProperty("user.home"))
        val os = System.getProperty("os.name").lowercase()
        return when {
            "win" in os -> home.resolve("AppData/Roaming/Mozilla/Firefox/Profiles")
            "mac" in os || "darwin" in os -> home.resolve("Library/Application Support/Firefox/Profiles")
            else -> home.resolve(".mozilla/firefox")
        }.also { debugln { "[FirefoxCookies] profiles dir: $it" } }
    }
}
