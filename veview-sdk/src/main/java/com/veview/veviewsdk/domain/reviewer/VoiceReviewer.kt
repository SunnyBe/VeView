package com.veview.veviewsdk.domain.reviewer

import com.aallam.openai.client.OpenAI
import com.squareup.moshi.Moshi
import com.veview.veviewsdk.data.analysis.OpenAIAnalysisEngine
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Provide Voice reviewer with state of voice review
 */
interface VoiceReviewer {
    val state: StateFlow<VoiceReviewState>
    fun start(reviewContext: ReviewContext)
    fun stop()
    fun cancel()

    // DNF: This should be a factory if at all, impl details is been exposed here!
    companion object {
        @Suppress("LongParameterList")
        internal fun create(
            configProvider: ConfigProvider,
            dispatcherProvider: DispatcherProvider,
            coroutineScope: CoroutineScope,
            audioProviderFactory: AudioCaptureProvider.Factory,
            openAI: OpenAI,
            moshi: Moshi
        ): VoiceReviewer {
            return VoiceReviewerImpl(
                analysisEngine = OpenAIAnalysisEngine(openAI, moshi),
                configProvider = configProvider,
                coroutineScope = coroutineScope,
                coroutineDispatcher = dispatcherProvider,
                audioProviderFactory = audioProviderFactory
            )
        }
    }
}
