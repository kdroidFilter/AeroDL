import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Duration

fun main() = runBlocking {
    val wrapper = YtDlpWrapper().apply {
        // Optional: where to place the downloads
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    }

    // Ensure yt-dlp is present
    if (!wrapper.isAvailable()) {
        println("yt-dlp introuvable, téléchargement en cours…")
        if (!wrapper.downloadOrUpdate()) {
            println("❌ Impossible de préparer yt-dlp (vérifie les permissions et l’accès réseau).")
            return@runBlocking
        }
    } else {
        println("✅ yt-dlp version: ${wrapper.version()}")
        // Optional: background update check
        try {
            if (wrapper.hasUpdate()) {
                println("⬆️ Mise à jour disponible, téléchargement…")
                wrapper.downloadOrUpdate()
            }
        } catch (_: Exception) { /* non-fatal */ }
    }

    // Ensure ffmpeg
    if (!wrapper.ensureFfmpegAvailable()) {
        println("⚠️ FFmpeg indisponible (macOS doit être installé via PATH).")
    } else {
        println("✅ FFmpeg prêt: ${wrapper.ffmpegPath}")
    }

    // Download
    val url = "https://www.youtube.com/watch?v=UoywDs3YXOM"
    println("📥 Téléchargement de la vidéo: $url")

    val handle = wrapper.download(
        url,
        YtDlpWrapper.Options(
            format = "bestvideo+bestaudio/best",
            noCheckCertificate = true, // mets true si problèmes TLS côté réseau filtré
            timeout = Duration.ofMinutes(20), // coupe proprement si ça stagne trop longtemps
            extraArgs = listOf("--concurrent-fragments", "8") // exemple d’opt utile
        )
    ) { event ->
        when (event) {
            is Event.Started -> {
                println("▶️  Téléchargement démarré…")
            }
            is Event.Progress -> {
                val pct = event.percent?.let { String.format("%.1f", it) } ?: "?"
                print("\rProgression: $pct%")
            }
            is Event.Log -> {
                // Optionnel: décommente pour debug verbeux
                // println("\nLOG: ${event.line}")
            }
            is Event.NetworkProblem -> {
                println("\n🌐 Problème réseau détecté: ${event.detail}")
            }
            is Event.Error -> {
                println("\n❌ Erreur: ${event.message}")
                event.cause?.let { println("   ↳ Cause: ${it::class.simpleName}: ${it.message}") }
            }
            is Event.Completed -> {
                println("\n${if (event.success) "✅" else "❌"} Téléchargement terminé (exit code ${event.exitCode})")
                if (!event.success) {
                    println("   Astuces: vérifie la connexion, les certificats, ou ajoute --no-check-certificate / un proxy si besoin.")
                }
            }
            is Event.Cancelled -> {
                println("\n⏹️  Téléchargement annulé.")
            }
        }
    }

    // Wait for completion (or timeout/annulation)
    handle.process.waitFor()
}
