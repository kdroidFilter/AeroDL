package io.github.kdroidfilter.network

import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import coil3.util.Logger
import okhttp3.OkHttpClient

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
    fun createImageLoader(logger: Logger? = DebugLogger()): ImageLoader {
        return ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { createOkHttpClient() }))
            }
            .apply {
                logger?.let { logger(it) }
            }
            .build()
    }
}