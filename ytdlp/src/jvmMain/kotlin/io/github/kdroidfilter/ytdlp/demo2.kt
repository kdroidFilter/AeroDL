package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.ytdlp.core.Event
import java.util.concurrent.CountDownLatch

fun main() {
    val ytdlp = YtDlpWrapper().apply {
        // Optionnels :
        // ytDlpPath = "/chemin/vers/yt-dlp"
        // ffmpegPath = "/chemin/vers/ffmpeg" // sinon PATH
        // downloadDir = java.io.File(System.getProperty("user.dir"))
    }

    val url = "https://www.youtube.com/watch?v=UoywDs3YXOM"

    // 1) Sondage formats (un seul -F derrière)
    val table = ytdlp.probeAvailability(url, noCheckCertificate = true)
    println(table)

    // 2) URL progressive <= 720p si dispo
    val urlProgressive = ytdlp.getMediumQualityUrl(
        url = url,
        maxHeight = 720,
        noCheckCertificate = true
    )
    urlProgressive.onSuccess { println("Direct progressive URL (<=720p): $it") }
        .onFailure { println("No progressive URL <=720p: ${it.message}") }

    // 3) Télécharger exactement en 720p (merge auto si pas progressif)
    val completed = CountDownLatch(1)

    val handle = ytdlp.downloadMp4At(

        url = url,
        preset = YtDlpWrapper.Preset.P1080,
        outputTemplate = "%(title)s.%(ext)s", // fichier lisible
        noCheckCertificate = true,
        // extraArgs = listOf("--concurrent-fragments", "16"), // optionnel: accélérer
        onEvent = { ev ->
            when (ev) {
                is Event.Started       -> println("Started")
                is Event.Progress      -> println("Progress: ${ev.percent ?: "-"}%")
                is Event.Log           -> {} // println(ev.line) si tu veux plus de verbosité
                is Event.NetworkProblem-> println("Network: ${ev.detail}")
                is Event.Error         -> { println("Error: ${ev.message}"); }
                is Event.Cancelled     -> { println("Cancelled"); completed.countDown() }
                is Event.Completed     -> {
                    println("Completed: exit=${ev.exitCode}, ok=${ev.success}")
                    completed.countDown()
                }
            }
        }
    )

    // ⚠️ Bloquer jusqu'à la fin du process (sinon la JVM meurt avant la fin du download)
    completed.await()

    // (Facultatif) Double sécurité si tu préfères :
    // handle.process.waitFor()
}
