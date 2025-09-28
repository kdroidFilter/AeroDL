package io.github.kdroidfilter.ytdlp.demos

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.core.Event
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CompletableFuture

fun main() {
    println("🚀 Démarrage de la démo de téléchargement...")

    // Utilise runBlocking pour exécuter le code asynchrone dans un contexte bloquant (parfait pour une app console)
    runBlocking {
        // 1. Créer une instance du wrapper
        val ytDlpWrapper = YtDlpWrapper()
        // Définir le paramètre de manière globale pour toutes les opérations de ce wrapper
        ytDlpWrapper.noCheckCertificate = true


        // (Optionnel) Définir un dossier de téléchargement personnalisé
        val downloadsDir = File(System.getProperty("user.home"), "YtDlpWrapper_Downloads")
        downloadsDir.mkdirs()
        ytDlpWrapper.downloadDir = downloadsDir
        println("📂 Fichiers seront sauvegardés dans : ${downloadsDir.absolutePath}")

        // 2. Initialiser le wrapper. Ceci est crucial et doit être fait avant tout.
        println("🔄 Initialisation de yt-dlp et FFmpeg (peut prendre un moment la première fois)...")
        val initSuccess = ytDlpWrapper.initialize { event ->
            // Affiche les événements d'initialisation pour informer l'utilisateur
            when (event) {
                is YtDlpWrapper.InitEvent.DownloadingYtDlp -> println("    -> Téléchargement de yt-dlp...")
                is YtDlpWrapper.InitEvent.EnsuringFfmpeg -> println("    -> Vérification de FFmpeg...")
                is YtDlpWrapper.InitEvent.Completed -> if (event.success) println("✅ Initialisation terminée avec succès !")
                is YtDlpWrapper.InitEvent.Error -> System.err.println("❌ Erreur d'initialisation : ${event.message}")
                else -> {} // Ignorer les autres événements comme la progression pour rester concis
            }
        }

        if (!initSuccess) {
            println("🛑 Échec de l'initialisation. Le programme va s'arrêter.")
            return@runBlocking
        }

        // 3. Lancer le téléchargement
        // URL d'une vidéo de test libre de droits (Big Buck Bunny)
        val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm"
        println("\n🎬 Lancement du téléchargement pour : $videoUrl")

        // noCheckCertificate n'est plus nécessaire ici, car il est défini globalement
        val table = ytDlpWrapper.probeAvailability(videoUrl)
        println(table)

        // Un CompletableFuture est utilisé pour attendre la fin du téléchargement asynchrone
        val downloadFuture = CompletableFuture<Boolean>()

        ytDlpWrapper.downloadMp4At(
            // noCheckCertificate n'est plus nécessaire ici
            url = videoUrl,
            preset = YtDlpWrapper.Preset.P1080, // Spécifie la qualité 1080p
            onEvent = { event ->
                when (event) {
                    is Event.Started -> println("    -> Le processus de téléchargement a démarré.")
                    is Event.Progress -> {
                        // Affiche la progression sur une seule ligne pour une console propre
                        val progressPercent = event.percent ?: 0.0
                        print("\r    -> Progression : ${"%.1f".format(progressPercent)}%")
                    }
                    is Event.Completed -> {
                        println("\n    -> Téléchargement terminé.")
                        if(event.success) {
                            println("🎉 Succès !")
                            downloadFuture.complete(true)
                        } else {
                            System.err.println("    -> Le téléchargement s'est terminé mais a échoué (exit code: ${event.exitCode}).")
                            downloadFuture.complete(false)
                        }
                    }
                    is Event.Error -> {
                        System.err.println("\n❌ Erreur de téléchargement : ${event.message}")
                        downloadFuture.complete(false)
                    }
                    is Event.Cancelled -> {
                        println("\n C Annulé.")
                        downloadFuture.complete(false)
                    }
                    is Event.NetworkProblem -> {
                        System.err.println("\n🌐 Problème réseau : ${event.detail}")
                        downloadFuture.complete(false)
                    }
                    else -> {} // Ignorer les événements de log pour cette démo
                }
            }
        )

        // 4. Attendre le résultat
        val success = downloadFuture.get() // Bloque le thread principal jusqu'à ce que le futur soit complété

        if (success) {
            println("\n👍 Le fichier a été téléchargé avec succès.")
        } else {
            println("\n👎 Une erreur est survenue pendant le téléchargement.")
        }
    }
}