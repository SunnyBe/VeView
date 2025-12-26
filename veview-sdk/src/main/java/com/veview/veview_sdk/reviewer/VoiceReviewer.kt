package com.veview.veview_sdk.reviewer

import android.content.Context
import com.veview.veview_sdk.configs.ConfigProvider
import com.veview.veview_sdk.VoiceReviewState
import com.veview.veview_sdk.analysis.OpenAIAnalysisEngine
import com.veview.veview_sdk.audiocapture.AndroidAudioCaptureProvider
import com.veview.veview_sdk.audiocapture.AudioCaptureProvider
import com.veview.veview_sdk.coroutine.DispatcherProvider
import com.veview.veview_sdk.model.ReviewContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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