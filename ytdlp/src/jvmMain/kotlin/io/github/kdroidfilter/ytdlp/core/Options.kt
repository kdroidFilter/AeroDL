package io.github.kdroidfilter.ytdlp.core

import java.time.Duration

data class Options(
    val format: String? = null,
    val outputTemplate: String? = null,
    val noCheckCertificate: Boolean = false,
    val extraArgs: List<String> = emptyList(),
    val timeout: Duration? = Duration.ofMinutes(30)
)
