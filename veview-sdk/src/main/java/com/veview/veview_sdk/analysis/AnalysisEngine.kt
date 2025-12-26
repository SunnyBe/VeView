package com.veview.veview_sdk.analysis

import com.veview.veview_sdk.model.VoiceReview
import java.io.File

interface AnalysisEngine {
    suspend fun analyze(audioFile: File): VoiceReview
}