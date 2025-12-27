package com.veview.veviewsdk.presentation

import android.content.Context
import androidx.annotation.MainThread
import com.veview.veviewsdk.data.audiocapture.AndroidAudioCaptureProvider
import com.veview.veviewsdk.data.configs.LocalConfigProviderImpl
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.data.coroutine.DefaultDispatcherProvider
import com.veview.veviewsdk.domain.reviewer.VoiceReviewer
import com.veview.veviewsdk.presentation.VeViewSDK.Companion.init
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import timber.log.Timber

/**
 * The main entry point for the VeView SDK.
 * This class is responsible for configuring and creating instances of [VoiceReviewer].
 *
 * Use the [Builder] to construct a configured instance.
 *
 * @param apiKey Your public API key.
 * @param okHttpClient A custom OkHttpClient for network requests.
 */
class VeViewSDK private constructor(
    private val apiKey: String,
    private val okHttpClient: OkHttpClient,
    private val isDebug: Boolean = false
) {

    /**
     * Creates a new [com.veview.veviewsdk.domain.reviewer.VoiceReviewer] instance.
     * This instance is independent and can be used concurrently with other instances.
     * @param context The Android Context.
     * @return A new instance of VoiceReviewer.
     */
    fun newAudioReviewer(
        context: Context,
        config: VoiceReviewConfig? = null
    ): VoiceReviewer {
        initTooling()

        Timber.tag(LOG_TAG).i("Creating VoiceReviewer")
        val dispatcherProvider = DefaultDispatcherProvider
        val reviewerScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
        val appConfigProvider = LocalConfigProviderImpl(context, config)
        return VoiceReviewer.create(
            context = context,
            apiKey = this.apiKey,
            okHttpClient = this.okHttpClient,
            configProvider = appConfigProvider,
            dispatcherProvider = dispatcherProvider,
            coroutineScope = reviewerScope,
            audioProviderFactory = AndroidAudioCaptureProvider.apply { initialize(context) }
        )
    }

    private fun initTooling() {
        if (isDebug) Timber.plant(Timber.DebugTree()) else Timber.plant()
    }

    /**
     * A builder for constructing [VeViewSDK] instances with custom configurations.
     * This allows for a flexible and clear setup.
     * @param apiKey Your public API key, required for all configurations.
     */
    class Builder(private val apiKey: String, isDebug: Boolean = false) {

        private var okHttpClient: OkHttpClient? = null
        private var debuggable: Boolean = isDebug

        /**
         * Sets a custom OkHttpClient for the SDK to use for network requests.
         * For production use, it is recommended to provide a client with
         * appropriate timeouts, caching, and interceptors as needed.
         * @param client The OkHttpClient instance.
         */
        fun setOkHttpClient(client: OkHttpClient) = apply {
            this.okHttpClient = client
        }

        /**
         * Enables debug mode for the SDK, activating verbose logging and additional checks.
         * Alternatively, debug mode can be set via the [Builder] constructor.
         */
        fun setDebug() = apply { debuggable = true }

        /**
         * Builds and returns a configured [VeViewSDK] instance.
         */
        fun build(): VeViewSDK {
            val finalOkHttpClient = this.okHttpClient ?: OkHttpClient()

            return VeViewSDK(
                apiKey = apiKey,
                okHttpClient = finalOkHttpClient,
                isDebug = debuggable
            )
        }
    }

    companion object {
        private const val LOG_TAG = "VeViewSDK"
        private var instance: VeViewSDK? = null

        /**
         * Initializes the SDK with a default configuration and sets it as a global singleton.
         * This method is a convenience for simple use cases. For advanced configuration,
         * it is recommended to use the [Builder] and manage your own instance.
         *
         * This method must be called on the main thread.
         *
         * @param apiKey Your public API key.
         * @throws IllegalStateException if called more than once.
         */
        @MainThread
        fun init(apiKey: String, isDebug: Boolean = false) {
            check(instance == null) {
                "VeViewSDK.init() already called. For a new instance use the Builder."
            }
            instance = Builder(apiKey, isDebug = isDebug).build()
        }

        /**
         * Retrieves the singleton instance of the VeView SDK.
         * @return The configured [VeViewSDK] instance.
         * @throws IllegalStateException if [init] has not been called first.
         */
        fun getInstance(): VeViewSDK {
            check(instance != null) { "VeViewSDK.getInstance() called before VeViewSDK.init()." }
            return instance!!
        }
    }
}
