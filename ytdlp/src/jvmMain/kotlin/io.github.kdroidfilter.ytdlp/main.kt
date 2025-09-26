package io.github.kdroidfilter.ytdlp

import io.github.kdroidfilter.platformtools.releasefetcher.github.GitHubReleaseFetcher
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.concurrent.thread

fun main() = runBlocking {
    println("=== YtDlpWrapper Example ===\n")
    
    // Créer le fetcher pour GitHub (pour télécharger yt-dlp depuis le repo officiel)
    val fetcher = GitHubReleaseFetcher(
        owner = "yt-dlp",
        repo = "yt-dlp"
    )
    
    // Créer l'instance du wrapper
    val wrapper = YtDlpWrapper(fetcher)
    
    // Configurer le répertoire de téléchargement (optionnel)
    val downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp-test").apply {
        if (!exists()) mkdirs()
    }
    wrapper.downloadDir = downloadDir
    
    // 1. Vérifier si yt-dlp est disponible
    println("📋 Vérification de yt-dlp...")
    val currentVersion = wrapper.version()
    
    if (currentVersion != null) {
        println("✅ yt-dlp trouvé, version: $currentVersion")
        
        // Vérifier s'il y a une mise à jour disponible
        val hasUpdate = wrapper.hasUpdate()
        if (hasUpdate) {
            println("🔄 Une mise à jour est disponible!")
            downloadAndInstall(wrapper)
        } else {
            println("👍 yt-dlp est à jour")
        }
    } else {
        println("❌ yt-dlp non trouvé, téléchargement nécessaire...")
        downloadAndInstall(wrapper)
    }
    
    // 2. Vérifier à nouveau après l'installation
    val finalVersion = wrapper.version()
    if (finalVersion == null) {
        println("❌ Impossible d'installer yt-dlp")
        return@runBlocking
    }
    
    println("\n✅ yt-dlp prêt! Version: $finalVersion")
    println("📁 Répertoire de téléchargement: ${downloadDir.absolutePath}")
    
    // 3. Télécharger une vidéo sample
    println("\n=== Téléchargement d'une vidéo sample ===\n")
    
    // URL d'une vidéo de test courte
    val sampleUrl = "https://www.youtube.com/watch?v=UoywDs3YXOM"
    // Alternative si YouTube ne fonctionne pas:
    // val sampleUrl = "https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_1mb.mp4"
    
    // Options de téléchargement
    val options = YtDlpWrapper.Options(
        format = "best[height<=720]",  // Limiter à 720p max pour un téléchargement rapide
        outputTemplate = "%(title)s_%(height)sp.%(ext)s",  // Nom personnalisé
        noCheckCertificate = true,
        extraArgs = listOf(
            "--quiet",           // Mode silencieux (moins de logs)
            "--progress",        // Afficher la progression
            "--no-playlist"      // Ne pas télécharger la playlist entière
        )
    )
    
    // Lancer le téléchargement dans un thread séparé (car c'est bloquant)
    val downloadThread = thread {
        println("🚀 Démarrage du téléchargement: $sampleUrl")
        
        val handle = wrapper.download(
            url = sampleUrl,
            options = options,
            onEvent = { event ->
                when (event) {
                    is YtDlpWrapper.Event.Started -> {
                        println("▶️ Téléchargement démarré...")
                    }
                    
                    is YtDlpWrapper.Event.Progress -> {
                        event.percent?.let { pct ->
                            val progressBar = createProgressBar(pct)
                            print("\r📥 Progression: $progressBar ${String.format("%.1f", pct)}%")
                        }
                    }
                    
                    is YtDlpWrapper.Event.Log -> {
                        // Afficher uniquement les logs importants
                        if (event.line.contains("Destination:") || 
                            event.line.contains("ERROR") ||
                            event.line.contains("WARNING")) {
                            println("\n📝 ${event.line}")
                        }
                    }
                    
                    is YtDlpWrapper.Event.Completed -> {
                        println("\n✅ Téléchargement terminé! (Code: ${event.exitCode})")
                        if (event.exitCode == 0) {
                            println("📁 Fichier téléchargé dans: ${downloadDir.absolutePath}")
                            listDownloadedFiles(downloadDir)
                        } else {
                            println("⚠️ Le téléchargement s'est terminé avec un code d'erreur")
                        }
                    }
                    
                    is YtDlpWrapper.Event.Error -> {
                        println("\n❌ Erreur: ${event.message}")
                        event.cause?.printStackTrace()
                    }
                    
                    is YtDlpWrapper.Event.Cancelled -> {
                        println("\n🛑 Téléchargement annulé")
                    }
                }
            }
        )
        
        // Exemple: annuler après 30 secondes (décommenter si nécessaire)
        // Thread.sleep(30000)
        // if (!handle.isCancelled) {
        //     println("\n⏰ Timeout - annulation du téléchargement...")
        //     handle.cancel()
        // }
    }
    
    // Attendre la fin du téléchargement
    downloadThread.join()
    
    println("\n\n=== Exemple terminé ===")
}

/**
 * Télécharge et installe le binaire yt-dlp
 */
suspend fun downloadAndInstall(wrapper: YtDlpWrapper) {
    println("📦 Téléchargement du binaire yt-dlp...")
    
    val success = withContext(Dispatchers.IO) {
        wrapper.downloadBinary()
    }
    
    if (success) {
        println("✅ Binaire téléchargé et installé avec succès!")
        println("📍 Chemin: ${wrapper.ytDlpPath}")
    } else {
        println("❌ Échec du téléchargement du binaire")
    }
}

/**
 * Crée une barre de progression visuelle
 */
fun createProgressBar(percent: Double): String {
    val width = 30
    val filled = (percent / 100.0 * width).toInt()
    val empty = width - filled
    return "[" + "█".repeat(filled) + "░".repeat(empty) + "]"
}

/**
 * Liste les fichiers téléchargés dans le répertoire
 */
fun listDownloadedFiles(dir: File) {
    val files = dir.listFiles { file -> 
        file.isFile && (file.extension in listOf("mp4", "webm", "mkv", "mp3", "m4a"))
    }
    
    if (!files.isNullOrEmpty()) {
        println("\n📂 Fichiers dans le répertoire:")
        files.forEach { file ->
            val sizeMB = file.length() / (1024.0 * 1024.0)
            println("  • ${file.name} (${String.format("%.2f", sizeMB)} MB)")
        }
    }
}

/**
 * Exemple additionnel: Télécharger uniquement l'audio
 */
fun downloadAudioOnly(wrapper: YtDlpWrapper, url: String) {
    val audioOptions = YtDlpWrapper.Options(
        format = "bestaudio/best",
        outputTemplate = "%(title)s.%(ext)s",
        extraArgs = listOf(
            "--extract-audio",           // Extraire l'audio
            "--audio-format", "mp3",      // Convertir en MP3
            "--audio-quality", "192K",    // Qualité audio
            "--embed-thumbnail",          // Intégrer la miniature
            "--add-metadata"              // Ajouter les métadonnées
        )
    )
    
    thread {
        println("🎵 Téléchargement audio uniquement...")
        wrapper.download(url, audioOptions) { event ->
            when (event) {
                is YtDlpWrapper.Event.Progress -> {
                    event.percent?.let { 
                        print("\r🎵 Audio: ${String.format("%.1f", it)}%")
                    }
                }
                is YtDlpWrapper.Event.Completed -> {
                    if (event.exitCode == 0) {
                        println("\n✅ Audio téléchargé avec succès!")
                    }
                }
                else -> { /* gérer autres événements */ }
            }
        }
    }.join()
}