package io.github.kdroidfilter.ytdlpgui.core.platform.network

import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import okhttp3.OkHttpClient

/**
 * Provides Coil ImageLoader configuration that uses native OS certificate stores.
 */
object CoilConfig {

    /**
     * Creates an OkHttpClient configured with native trusted roots
     */
    fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .sslSocketFactory(TrustedRootsSSL.socketFactory, TrustedRootsSSL.trustManager)
            .build()
    }

    /**
     * Creates a Coil ImageLoader configured with native trusted roots
     */
    fun createImageLoader(): ImageLoader {
        return ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { createOkHttpClient() }))
            }
            .logger(DebugLogger())
            .build()
    }
}