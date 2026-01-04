package com.veview.veviewsdk.data.analysis.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
    @Json(name = "summary") val summary: String = "",
    @Json(name = "rating") val rating: Int = 1,
    @Json(name = "pros") val pros: List<String> = emptyList(),
    @Json(name = "cons") val cons: List<String> = emptyList()
)
