package com.veview.veview_sdk

import android.content.Context
import androidx.annotation.MainThread
import com.veview.veview_sdk.VeViewSDK.Companion.init
import com.veview.veview_sdk.audiocapture.AndroidAudioCaptureProvider
import com.veview.veview_sdk.configs.ConfigProvider
import com.veview.veview_sdk.configs.VoiceReviewConfig
import com.veview.veview_sdk.coroutine.DispatcherProvider
import com.veview.veview_sdk.reviewer.VoiceReviewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import timber.log.Timber

/**
 * The main entry point for the VeView SDK.
 * This class is responsible for configuring and creating instances of [com.veview.veview_sdk.reviewer.VoiceReviewer].
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
     * Creates a new [com.veview.veview_sdk.reviewer.VoiceReviewer] instance.
     *
     * @param context The Android Context.
     * @return A new instance of VoiceReviewer.
     */
    fun newAudioReviewer(
        context: Context,
        config: VoiceReviewConfig? = null
    ): VoiceReviewer {
        initTooling()

        Timber.i("Creating VoiceReviewer")
        val dispatcherProvider = DispatcherProvider.LIVE
        val reviewerScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
        val appConfigProvider = if (isDebug) {
            ConfigProvider.localProvider(context)
        } else {
            ConfigProvider.defaultProvider(context, config)
        }
        return VoiceReviewer.create(
            context = context,
            apiKey = this.apiKey,
            okHttpClient = this.okHttpClient,
            configProvider = appConfigProvider,
            dispatcherProvider = dispatcherProvider,
            coroutineScope = reviewerScope,
            audioCaptureProvider = AndroidAudioCaptureProvider.create(
                context = context,
                coroutineScope = reviewerScope,
                dispatcherProvider = dispatcherProvider
            )
        )
    }

    private fun initTooling() {
        if (isDebug) Timber.plant(Timber.DebugTree()) else Timber.plant()
    }

    /**
     * A builder for constructing [VeViewSDK] instances with custom configurations.
     * This allows for a flexible and clear setup.
     *
     * @param apiKey Your public API key, required for all configurations.
     */
    class Builder(private val apiKey: String, private val isDebug: Boolean = false) {

        private var okHttpClient: OkHttpClient? = null
        private var isDebugable: Boolean = isDebug

        /**
         * Specifies a custom [OkHttpClient] to be used for all network operations.
         * Providing your own client is recommended for production apps to share resources
         * like connection pools and thread dispatchers.
         * @param client The OkHttpClient instance.
         */
        fun okHttpClient(client: OkHttpClient) = apply {
            this.okHttpClient = client
        }

        fun setDebug() = apply { isDebugable = true }

        /**
         * Builds and returns a configured [VeViewSDK] instance.
         */
        fun build(): VeViewSDK {
            val finalOkHttpClient = this.okHttpClient ?: OkHttpClient()

            return VeViewSDK(
                apiKey = apiKey,
                okHttpClient = finalOkHttpClient,
                isDebug = isDebugable
            )
        }
    }

    companion object {
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
            check(instance == null) { "VeViewSDK.init() has already been called. To create a new instance with a different configuration, use the Builder." }
            instance = Builder(apiKey, isDebug = isDebug).build()
        }

        /**
         * Retrieves the singleton instance of the VeView SDK.
         * @return The configured [VeViewSDK] instance.
         * @throws IllegalStateException if [init] has not been called first.
         */
        fun getInstance(): VeViewSDK {
            return instance
                ?: throw IllegalStateException("VeViewSDK.getInstance() called before VeViewSDK.init().")
        }
    }
}