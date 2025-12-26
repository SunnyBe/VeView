package com.veview.veview_sdk.domain.contracts

import com.veview.veview_sdk.domain.model.VoiceReview
import java.io.File

interface AnalysisEngine {
    suspend fun analyze(audioFile: File): VoiceReview
}