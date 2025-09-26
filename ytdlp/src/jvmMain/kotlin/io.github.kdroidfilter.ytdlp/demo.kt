package io.github.kdroidfilter.ytdlp

fun main() {
    val wrapper = YtDlpWrapper()

    val videoUrl = "https://www.youtube.com/watch?v=UoywDs3YXOM"

    // Active no-check-certificate
    val result = wrapper.getMediumQualityUrl(
        url = videoUrl,
        maxHeight = 480,
        preferredExts = listOf("mp4", "webm"),
        noCheckCertificate = true
    )

    if (result.isSuccess) {
        println("✅ Direct URL (no cert check):")
        println(result.getOrNull())
    } else {
        println("❌ Error: ${result.exceptionOrNull()?.message}")
    }
}
