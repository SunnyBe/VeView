package com.veview.veview_sdk.domain.reviewer

import android.content.Context
import com.veview.veview_sdk.presentation.voice_review.VoiceReviewState
import com.veview.veview_sdk.data.analysis.OpenAIAnalysisEngine
import com.veview.veview_sdk.domain.contracts.AudioCaptureProvider
import com.veview.veview_sdk.domain.contracts.ConfigProvider
import com.veview.veview_sdk.domain.contracts.DispatcherProvider
import com.veview.veview_sdk.domain.model.ReviewContext
import com.veview.veview_sdk.domain.reviewer.VoiceReviewerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

// TODO: Provide doc
interface VoiceReviewer {
    val state: StateFlow<VoiceReviewState>
    fun start(reviewContext: ReviewContext)
    fun stop()
    fun cancel()

    companion object {
        internal fun create(
            context: Context,
            apiKey: String,
            okHttpClient: OkHttpClient,
            configProvider: ConfigProvider,
            dispatcherProvider: DispatcherProvider,
            coroutineScope: CoroutineScope,
            audioCaptureProvider: AudioCaptureProvider
        ): VoiceReviewer {
            return VoiceReviewerImpl(
                apiKey = apiKey,
                analysisEngine = OpenAIAnalysisEngine(apiKey, okHttpClient),
                configProvider = configProvider,
                coroutineScope = coroutineScope,
                coroutineDispatcher = dispatcherProvider,
                audioCaptureProvider = audioCaptureProvider
            )
        }
    }
}