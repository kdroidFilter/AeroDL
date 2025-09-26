import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.YtDlpWrapper.InitEvent
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Duration

fun main() = runBlocking {
    val wrapper = YtDlpWrapper().apply {
        // Dossier de téléchargement par défaut (modifiable)
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    }

    // Nouvelle API d'initialisation non bloquante avec événements pour l'UI
    println("🔧 Initialisation de yt-dlp/ffmpeg…")
    val initOk = wrapper.initialize { ev ->
        when (ev) {
            is InitEvent.CheckingYtDlp -> println("🔍 Vérification de yt-dlp…")
            is InitEvent.DownloadingYtDlp -> println("⬇️ Téléchargement de yt-dlp…")
            is InitEvent.UpdatingYtDlp -> println("⬆️ Mise à jour de yt-dlp…")
            is InitEvent.EnsuringFfmpeg -> println("🎬 Vérification de FFmpeg…")
            is InitEvent.YtDlpProgress -> {
                val pct = ev.percent?.let { String.format("%.1f", it) } ?: "?"
                print("\r⬇️ yt-dlp: $pct%")
            }
            is InitEvent.FfmpegProgress -> {
                val pct = ev.percent?.let { String.format("%.1f", it) } ?: "?"
                print("\r🎬 FFmpeg: $pct%")
            }
            is InitEvent.Error -> {
                println("\n⚠️ Init: ${ev.message}")
                ev.cause?.let { println("   ↳ ${it::class.simpleName}: ${it.message}") }
            }
            is InitEvent.Completed -> println(if (ev.success) "\n✅ Init ok" else "\n❌ Init échouée")
        }
    }
    if (!initOk) {
        println("Arrêt car initialisation impossible.")
        return@runBlocking
    }

    println("✅ yt-dlp version: ${wrapper.version() ?: "inconnue"}")
    println("✅ FFmpeg: ${wrapper.ffmpegPath ?: "(via PATH ou gestion interne)"}")

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
            is Event.Started -> println("▶️  Téléchargement démarré…")
            is Event.Progress -> {
                val pct = event.percent?.let { String.format("%.1f", it) } ?: "?"
                print("\rProgression: $pct%")
            }
            is Event.Log -> { /* println("\nLOG: ${event.line}") */ }
            is Event.NetworkProblem -> println("\n🌐 Problème réseau détecté: ${event.detail}")
            is Event.Error -> {
                println("\n❌ Erreur: ${event.message}")
                event.cause?.let { println("   ↳ Cause: ${it::class.simpleName}: ${it.message}") }
            }
            is Event.Completed -> {
                println("\n${if (event.success) "✅" else "❌"} Téléchargement terminé (exit code ${event.exitCode})")
                if (!event.success) println("   Astuces: vérifie la connexion, les certificats, ou ajoute --no-check-certificate / un proxy si besoin.")
            }
            is Event.Cancelled -> println("\n⏹️  Téléchargement annulé.")
        }
    }

    // Wait for completion (or timeout/annulation)
    handle.process.waitFor()
}
