package com.veview.veview_sdk.audiocapture

import com.veview.veview_sdk.configs.VoiceReviewConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AudioCaptureProvider {
    val audioStream: Flow<ByteArray>

    suspend fun startRecording(fileName: String, config: VoiceReviewConfig): Pair<File, Job>
//    suspend fun startRecording(fileName: String, config: VoiceReviewConfig): File
    fun stopRecording()
}