package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.model.AudioRecordState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface AudioCaptureProvider {

    fun startRecording(
        fileName: String,
        config: VoiceReviewConfig,
        duration: Duration
    ): Flow<AudioRecordState>

    fun stopRecording()
    fun cancel()

    interface Factory {
        fun create(scope: CoroutineScope): AudioCaptureProvider
    }
}
