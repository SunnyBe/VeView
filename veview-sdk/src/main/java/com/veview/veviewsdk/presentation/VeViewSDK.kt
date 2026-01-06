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
import com.veview.veviewsdk.domain.model.VoiceReview
import com.veview.veviewsdk.domain.reviewer.VoiceReviewer
import com.veview.veviewsdk.domain.reviewer.VoiceReviewerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * The main entry point for the VeView SDK.
 * This class is responsible for configuring and creating instances of [VoiceReviewer] for both
 * the default [VoiceReview] model and custom, client-defined data models.
 *
 * Use the [Builder] to construct a configured instance.
 */
@Keep
class VeViewSDK private constructor(
    private val apiKey: String,
    private val isDebug: Boolean = false
) {

    internal val openAI by lazy {
        val openAiConfig = OpenAIConfig(
            token = this.apiKey,
            timeout = Timeout(socket = 60.seconds),
            logging = LoggingConfig(if (isDebug) LogLevel.All else LogLevel.None),
            retry = RetryStrategy(maxRetries = 1, maxDelay = 60.seconds)
        )
        OpenAI(config = openAiConfig)
    }

    internal val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    internal val dispatcherProvider = DefaultDispatcherProvider
    internal val reviewerScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    /**
     * Creates a new [VoiceReviewer] instance for the default [VoiceReview] data model.
     * This is the standard implementation for getting a general-purpose review analysis.
     *
     * @param context The Android Context.
     * @param config An optional [VoiceReviewConfig] to customize the behavior of the session.
     * @return A new instance of [VoiceReviewer] that provides a [VoiceReview] result.
     */
    fun newAudioReviewer(
        context: Context,
        config: VoiceReviewConfig? = null
    ): VoiceReviewer<VoiceReview> {
        initTooling()

        Timber.tag(LOG_TAG).i("Creating standard VoiceReviewer")
        return VoiceReviewerImpl.create(
            configProvider = LocalConfigProviderImpl(context, config),
            dispatcherProvider = dispatcherProvider,
            coroutineScope = reviewerScope,
            audioProviderFactory = AndroidAudioCaptureProvider.apply { initialize(context) },
            openAI = openAI,
            moshi = moshi,
            responseType = VoiceReview::class.java
        )
    }

    /**
     * Creates a new [VoiceReviewer] instance for a custom, client-defined data model.
     * This allows clients to define their own data structure and receive a typed result from the analysis.
     *
     * @param T The client-defined data class for the custom analysis response.
     * @param context The Android Context.
     * @param responseType The class of the custom data model (e.g., `MyCustomReview::class.java`),
     *   used for deserializing the analysis response.
     * @param config An optional [VoiceReviewConfig] to customize the behavior, such as providing a custom prompt.
     * @return A new instance of [VoiceReviewer] typed to the custom data model [T].
     */
    fun <T> newCustomAudioReviewer(
        context: Context,
        responseType: Class<T>,
        config: VoiceReviewConfig? = null
    ): VoiceReviewer<T> {
        initTooling()

        Timber.tag(LOG_TAG).i("Creating custom VoiceReviewer for type ${responseType.simpleName}")
        return VoiceReviewerImpl.create(
            configProvider = LocalConfigProviderImpl(context, config),
            dispatcherProvider = dispatcherProvider,
            coroutineScope = reviewerScope,
            audioProviderFactory = AndroidAudioCaptureProvider.apply { initialize(context) },
            openAI = openAI,
            moshi = moshi,
            responseType = responseType
        )
    }

    private fun initTooling() {
        if (isDebug) Timber.plant(Timber.DebugTree()) else Timber.plant()
    }

    /**
     * A builder for constructing [VeViewSDK] instances with custom configurations.
     *
     * @param apiKey Your public API key from the OpenAI Platform. This is required and cannot be blank.
     * @param isDebug Enables debug mode for the SDK, activating verbose logging.
     */
    @Keep
    class Builder(private val apiKey: String, private val isDebug: Boolean = false) {

        /**
         * Builds and returns a configured [VeViewSDK] instance.
         *
         * @throws IllegalStateException if the API key is blank.
         */
        fun build(): VeViewSDK {
            check(apiKey.isNotBlank()) { "API key cannot be blank." }

            return VeViewSDK(
                apiKey = apiKey,
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
         * @param apiKey Your public API key from the OpenAI Platform.
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
