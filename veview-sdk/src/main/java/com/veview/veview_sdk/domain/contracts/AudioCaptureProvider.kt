package com.veview.veview_sdk.domain.contracts

import com.veview.veview_sdk.data.configs.VoiceReviewConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AudioCaptureProvider {
    val audioStream: Flow<ByteArray>

    suspend fun startRecording(fileName: String, config: VoiceReviewConfig): Pair<File, Job>
    fun stopRecording()
}