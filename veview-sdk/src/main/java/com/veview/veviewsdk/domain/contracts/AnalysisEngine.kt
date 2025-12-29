package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.domain.model.VoiceReview
import java.io.File

interface AnalysisEngine {
    suspend fun analyze(audioFile: File): VoiceReview
    // For customized analysis responses for payed clients. After MVP release.
//    suspend fun <T> customAnalyze(audioFile: File, responseClass: Class<T>): T
}
