package com.veview.veviewsdk.presentation

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.MainThread
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.RetryStrategy
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.veview.veviewsdk.data.audiocapture.AndroidAudioCaptureProvider
import com.veview.veviewsdk.data.configs.LocalConfigProviderImpl
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.data.coroutine.DefaultDispatcherProvider
import com.veview.veviewsdk.domain.reviewer.VoiceReviewer
import com.veview.veviewsdk.domain.reviewer.VoiceReviewerImpl
import com.veview.veviewsdk.presentation.VeViewSDK.Companion.init
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * The main entry point for the VeView SDK.
 * This class is responsible for configuring and creating instances of [VoiceReviewer].
 *
 * Use the [Builder] to construct a configured instance.
 */
@Keep
class VeViewSDK private constructor(
    private val apiKey: String,
    private val okHttpClient: OkHttpClient,
    private val isDebug: Boolean = false
) {

    val openAI by lazy {
        val openAiConfig = OpenAIConfig(
            token = this.apiKey,
            timeout = Timeout(socket = 60.seconds),
            logging = LoggingConfig(if (isDebug) LogLevel.All else LogLevel.None),
            retry = RetryStrategy(maxRetries = 1, maxDelay = 60.seconds)
        )
        OpenAI(config = openAiConfig)
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Creates a new [VoiceReviewer] instance.
     * This instance is independent and can be used concurrently with other instances.
     *
     * @param context The Android Context.
     * @param config An optional [VoiceReviewConfig] to customize the behavior of the [VoiceReviewer].
     * @return A new instance of [VoiceReviewer].
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

        return VoiceReviewerImpl.create(
            configProvider = appConfigProvider,
            dispatcherProvider = dispatcherProvider,
            coroutineScope = reviewerScope,
            audioProviderFactory = AndroidAudioCaptureProvider.apply { initialize(context) },
            openAI = openAI,
            moshi = moshi
        )
    }

    private fun initTooling() {
        if (isDebug) Timber.plant(Timber.DebugTree()) else Timber.plant()
    }

    /**
     * A builder for constructing [VeViewSDK] instances with custom configurations.
     * This allows for a flexible and clear setup.
     *
     * @param apiKey Your public API key. This is required and cannot be blank.
     * @param isDebug Enables debug mode for the SDK, activating verbose logging.
     */
    @Keep
    class Builder(private val apiKey: String, private val isDebug: Boolean = false) {

        private var okHttpClient: OkHttpClient? = null

        /**
         * Sets a custom OkHttpClient for the SDK to use for network requests.
         * For production use, it is recommended to provide a client with
         * appropriate timeouts, caching, and interceptors as needed.
         *
         * @param client The OkHttpClient instance.
         */
        fun setOkHttpClient(client: OkHttpClient) = apply {
            this.okHttpClient = client
        }

        /**
         * Builds and returns a configured [VeViewSDK] instance.
         *
         * @throws IllegalStateException if the API key is blank.
         */
        fun build(): VeViewSDK {
            check(apiKey.isNotBlank()) { "API key cannot be blank." }
            val finalOkHttpClient = this.okHttpClient ?: OkHttpClient()

            return VeViewSDK(
                apiKey = apiKey,
                okHttpClient = finalOkHttpClient,
                isDebug = isDebug
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
         * @param isDebug Enables debug mode for the SDK, activating verbose logging.
         * @throws IllegalStateException if called more than once or if the API key is blank.
         */
        @MainThread
        fun init(apiKey: String, isDebug: Boolean = false) {
            check(instance == null) {
                "VeViewSDK.init() has already been called. For a new instance, use the Builder."
            }
            instance = Builder(apiKey, isDebug = isDebug).build()
        }

        /**
         * Retrieves the singleton instance of the VeView SDK.
         *
         * @return The configured [VeViewSDK] instance.
         * @throws IllegalStateException if [init] has not been called first.
         */
        fun getInstance(): VeViewSDK {
            check(instance != null) { "VeViewSDK.getInstance() called before VeViewSDK.init()." }
            return instance!!
        }
    }
}
