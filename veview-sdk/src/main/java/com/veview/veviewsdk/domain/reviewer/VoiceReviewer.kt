package com.veview.veviewsdk.domain.reviewer

import android.content.Context
import com.veview.veviewsdk.data.analysis.OpenAIAnalysisEngine
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

/**
 * Provide Voice reviewer with state of voice review
 */
interface VoiceReviewer {
    val state: StateFlow<VoiceReviewState>
    fun start(reviewContext: ReviewContext)
    fun stop()
    fun cancel()

    companion object {
        @Suppress("LongParameterList")
        internal fun create(
            context: Context,
            apiKey: String,
            okHttpClient: OkHttpClient,
            configProvider: ConfigProvider,
            dispatcherProvider: DispatcherProvider,
            coroutineScope: CoroutineScope,
            audioProviderFactory: AudioCaptureProvider.Factory
        ): VoiceReviewer {
            return VoiceReviewerImpl(
                apiKey = apiKey,
                analysisEngine = OpenAIAnalysisEngine(apiKey, okHttpClient),
                configProvider = configProvider,
                coroutineScope = coroutineScope,
                coroutineDispatcher = dispatcherProvider,
                audioProviderFactory = audioProviderFactory
            )
        }
    }
}
