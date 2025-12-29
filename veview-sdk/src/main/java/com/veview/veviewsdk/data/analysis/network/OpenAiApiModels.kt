package com.veview.veviewsdk.data.analysis.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.File

// --- Data classes for Whisper Transcription ---

@JsonClass(generateAdapter = true)
data class WhisperTranscriptionResponse(
    @Json(name = "text") val text: String
)

// --- Data classes for GPT Chat Completion ---

@JsonClass(generateAdapter = true)
data class GptChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<GptMessage>
)

@JsonClass(generateAdapter = true)
data class GptMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GptChatResponse(
    @Json(name = "choices") val choices: List<GptChoice>
)

@JsonClass(generateAdapter = true)
data class GptChoice(
    @Json(name = "message") val message: GptMessage
)

@JsonClass(generateAdapter = true)
data class GptAnalysisResponse(
    @Json(name = "transcript") val transcript: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "estimatedRating") val estimatedRating: Int,
    @Json(name = "pros") val pros: List<String>,
    @Json(name = "cons") val cons: List<String>,
    @Json(name = "audioFile") val audioFile: File?
)
