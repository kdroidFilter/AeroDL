package io.github.kdroidfilter.ytdlp

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("\n=== Testing Problematic YouTube Videos ===\n")

    // Initialize wrapper
    val wrapper = YtDlpWrapper()
    wrapper.initialize()

    val testUrls = listOf(
        "https://www.youtube.com/watch?v=xXLxSgqVO9c",
        "https://www.youtube.com/watch?v=3yNKLOPEyJA"
    )

    for (url in testUrls) {
        println("Testing: $url")

        val result = wrapper.getVideoInfoWithAllSubtitles(url)

        result.onSuccess { videoInfo ->
            println("  Title: ${videoInfo.title}")
            println("  Available resolutions: ${videoInfo.availableResolutions.keys.sorted()}")
            println("  Resolutions count: ${videoInfo.availableResolutions.size}")

            if (videoInfo.availableResolutions.isEmpty()) {
                println("  ⚠️ WARNING: NO RESOLUTIONS FOUND!")
            } else {
                videoInfo.availableResolutions.forEach { (height, avail) ->
                    println("    ${height}p - progressive: ${avail.progressive}, downloadable: ${avail.downloadable}")
                }
            }
        }.onFailure { error ->
            println("  ERROR: ${error.message}")
            error.printStackTrace()
        }

        println()
    }
}