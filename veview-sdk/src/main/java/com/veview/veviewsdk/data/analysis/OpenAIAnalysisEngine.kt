package com.veview.veviewsdk.data.analysis

import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.model.VoiceReview
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.io.File

/**
 * Implementation using OpenAI's Whisper + GPT.
 */
@Suppress("UnusedPrivateProperty")
internal class OpenAIAnalysisEngine(private val apiKey: String, private val okHttpClient: OkHttpClient) :
    AnalysisEngine {
    @Suppress("MagicNumber")
    override suspend fun analyze(audioFile: File): VoiceReview {
        // 1. Call Whisper API for transcription
        // 2. Call GPT API for structured summary
        // 3. Return VoiceReview object

        delay(1000) // Model delay in processing. Remove the suppressed check with this

        val result = VoiceReview(
            transcript = "Hello hello",
            summary = "I love this",
            estimatedRating = 3,
            pros = emptyList(),
            cons = emptyList(),
            audioFile = audioFile
        )
        return result
    }
}
