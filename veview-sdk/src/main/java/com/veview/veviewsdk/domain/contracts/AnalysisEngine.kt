package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.domain.model.VoiceReview
import java.io.File

interface AnalysisEngine {
    suspend fun analyze(audioFile: File): VoiceReview
}
