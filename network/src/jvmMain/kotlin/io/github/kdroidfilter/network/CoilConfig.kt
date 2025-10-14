package io.github.kdroidfilter.network

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import coil3.util.Logger
import coil3.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File
import okio.Path.Companion.toPath
import io.github.kdroidfilter.logging.LoggerConfig

/**
 * Provides Coil ImageLoader configuration that uses native OS certificate stores.
 */
object CoilConfig {

    /**
     * Creates an OkHttpClient configured with native trusted roots
     *
     * @return Configured OkHttpClient instance
     */
    fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .sslSocketFactory(TrustedRootsSSL.socketFactory, TrustedRootsSSL.trustManager)
            .build()
    }

    /**
     * Creates a Coil ImageLoader configured with native trusted roots
     *
     * @param logger Optional logger for debugging (default: DebugLogger)
     * @return Configured ImageLoader instance
     */
    fun createImageLoader(logger: Logger? = if (LoggerConfig.enabled) DebugLogger() else null): ImageLoader {
        val cacheBase = File(System.getProperty("user.home"), ".aerodl/coil_cache").apply { mkdirs() }
        return ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { createOkHttpClient() }))
            }
            // Cap memory cache to a conservative size to avoid excessive RAM usage
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(64L * 1024L * 1024L) // 64 MB
                    .build()
            }
            // Enable disk cache so thumbnails reload without keeping many bitmaps in RAM
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheBase.absolutePath.toPath())
                    .maxSizeBytes(256L * 1024L * 1024L) // 256 MB
                    .build()
            }
            .apply {
                logger?.let { logger(it) }
            }
            .build()
    }
}
