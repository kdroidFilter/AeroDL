import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.YtDlpWrapper.InitEvent

import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val wrapper = YtDlpWrapper().apply {
        downloadDir = File(System.getProperty("user.home"), "Downloads/yt-dlp")
    }
    // Définir le paramètre de manière globale pour toutes les opérations
    wrapper.noCheckCertificate = true


    // Initialisation
    println("🔧 Initialisation de yt-dlp/ffmpeg…")
    val initOk = wrapper.initialize { ev ->
        when (ev) {
            is InitEvent.CheckingYtDlp -> println("🔍 Vérification de yt-dlp…")
            is InitEvent.EnsuringFfmpeg -> println("🎬 Vérification de FFmpeg…")
            is InitEvent.Completed -> println(if (ev.success) "✅ Init ok" else "❌ Init échouée")
            else -> {} // Simplifier l'output
        }
    }

    if (!initOk) {
        println("Arrêt car initialisation impossible.")
        return@runBlocking
    }

    // =========================
    // TEST 1: Vidéo simple
    // =========================
    println("\n📹 TEST 1: Vidéo simple")
    val videoUrl = "https://ivan.canet.dev/talks/bordeauxkt.html#kotlin-beyond-the-jvm" // Me at the zoo

    // L'argument noCheckCertificate n'est plus nécessaire
    wrapper.getVideoInfo(videoUrl, timeoutSec = 60)
        .onSuccess { video ->
            println("✅ Vidéo trouvée:")
            println("  📝 Titre: ${video.title}")
            println("  👤 Uploader: ${video.uploader}")
            println("  ⏱️ Durée: ${video.duration}")
            println("  👁️ Vues: ${video.viewCount}")
            println("   Direct link ${video.directUrl}")
        }
        .onFailure {
            println("❌ Échec: ${it.message}")
            println(it.cause)
        }

    // =========================
    // TEST 2: Playlist YouTube
    // =========================
    println("\n📚 TEST 2: Playlist")
    val playlistUrls = listOf(
        "https://www.youtube.com/playlist?list=PLqsuMHtPTtp0qTyJ7Zl-ftZslOvPGJLTe",

        "https://www.youtube.com/watch?v=2g-5XqAbY9s&list=PLIId9bc1RIsGsLcYycVsX-4DC1Soswxbj",

        "https://www.youtube.com/watch?v=DMDEFfr98gg&list=PLIId9bc1RIsHXSq8xp3kf4IO_gCc14U3r"
    )

//    for (playlistUrl in playlistUrls) { // Tester seulement la première
//        println("\n🎵 Playlist: $playlistUrl")
//
//        wrapper.getPlaylistInfo(
//            playlistUrl,
//            extractFlat = true,  // Plus rapide, juste les métadonnées de base
//            timeoutSec = 60
//        ).onSuccess { playlist ->
//            println("✅ Playlist trouvée:")
//            println("  📝 Titre: ${playlist.title}")
//            println("  👤 Créateur: ${playlist.uploader}")
//            println("  📊 Nombre de vidéos: ${playlist.entryCount}")
//            println("  🎬 Premières vidéos:")
//            playlist.entries.take(5).forEachIndexed { index, video ->
//                println("    ${index + 1}. ${video.title}")
//                println("       URL: ${video.url}")
//            }
//        }.onFailure {
//            println("❌ Échec playlist: ${it.message}")
//        }
//    }

    // =========================
    // TEST 3: Chaîne YouTube
    // =========================
    println("\n📺 TEST 3: Chaînes YouTube")
    val channelUrls = listOf(


        "https://www.youtube.com/@PhilippLackner",
//        "https://www.youtube.com/channel/UCYZ0IYNeA_aCqlM9KIC_8DQ"
    )

    for (channelUrl in channelUrls) { // Tester seulement la première
        println("\n📺 Chaîne: $channelUrl")

        // Pour une chaîne, on récupère une liste de vidéos
        // L'argument noCheckCertificate n'est plus nécessaire
        wrapper.getVideoInfoList(
            channelUrl,
            maxEntries = 30,
            extractFlat = true,  // Plus rapide
            timeoutSec = 90
        ).onSuccess { videos ->
            println("✅ Vidéos de la chaîne:")
            println("  📊 ${videos.size} vidéos récupérées")
            videos.forEachIndexed { index, video ->
                println("  ${index + 1}. ${video.title}")
                println("     📅 Date: ${video.uploadDate}")
                println("     👁️ Vues: ${video.viewCount}")
                println("      Thumbnial ${video.duration}")
            }
        }.onFailure {
            println("❌ Échec chaîne: ${it.message}")
        }
    }

//    // =========================
//    // TEST 4: Playlist avec plus de détails (sans flat)
//    // =========================
//    println("\n🎬 TEST 4: Playlist détaillée (peut être lent)")
//    val shortPlaylist = "https://www.youtube.com/playlist?list=PLIId9bc1RIsHsrHIda0dpJB-LlVlEklFQ"
//
//    wrapper.getPlaylistInfo(
//        shortPlaylist,
//        extractFlat = false,  // Récupère TOUTES les infos (lent!)
//        timeoutSec = 120
//    ).onSuccess { playlist ->
//        println("✅ Playlist complète:")
//        println("  📝 Titre: ${playlist.title}")
//        playlist.entries.take(3).forEach { video ->
//            println("\n  🎥 ${video.title}")
//            println("     ⏱️ Durée: ${video.duration}")
//            println("     📺 Résolution: ${video.height}p")
//            println("     👤 Uploader: ${video.uploader}")
//            println("     🏷️ Tags: ${video.tags.take(5).joinToString(", ")}")
//            if (video.chapters.isNotEmpty()) {
//                println("     📑 Chapitres: ${video.chapters.size}")
//            }
//        }
//    }.onFailure {
//        println("❌ Échec: ${it.message}")
//    }
//
//    // =========================
//    // TEST 5: URLs spéciales
//    // =========================
//    println("\n🔗 TEST 5: URLs spéciales")
//
//    // Vidéo dans une playlist
//    val videoInPlaylist = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"
//
//    // Live stream (peut échouer si pas de live)
//    val liveUrl = "https://www.youtube.com/@LofiGirl/live"
//
//    // Shorts
//    val shortsUrl = "https://www.youtube.com/shorts/n0QNaym0jDI"
//
//    println("Testing vidéo dans playlist...")
//    wrapper.getVideoInfo(videoInPlaylist)
//        .onSuccess { println("  ✅ ${it.title}") }
//        .onFailure { println("  ❌ ${it.message}") }
}