package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AudioCaptureProvider {
    val audioStream: Flow<ByteArray>

    suspend fun startRecording(fileName: String, config: VoiceReviewConfig): File
    fun stopRecording()
    fun cancel()

    interface Factory {
        fun create(scope: CoroutineScope): AudioCaptureProvider
    }
}
