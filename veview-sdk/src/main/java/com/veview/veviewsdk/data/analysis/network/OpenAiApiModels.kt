package com.veview.veviewsdk.data.analysis.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Data classes for Whisper Transcription ---

@JsonClass(generateAdapter = true)
data class WhisperTranscriptionResponse(
    @field:Json(name = "text") val text: String
)

// --- Data classes for GPT Chat Completion ---

@JsonClass(generateAdapter = true)
data class GptChatRequest(
    @field:Json(name = "model") val model: String,
    @field:Json(name = "messages") val messages: List<GptMessage>
)

@JsonClass(generateAdapter = true)
data class GptMessage(
    @field:Json(name = "role") val role: String,
    @field:Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GptChatResponse(
    @field:Json(name = "choices") val choices: List<GptChoice>
)

@JsonClass(generateAdapter = true)
data class GptChoice(
    @field:Json(name = "message") val message: GptMessage
)

@JsonClass(generateAdapter = true)
data class GptAnalysisResponse(
    @field:Json(name = "summary") val summary: String = "",
    @field:Json(name = "rating") val rating: Int = 1,
    @field:Json(name = "pros") val pros: List<String> = emptyList(),
    @field:Json(name = "cons") val cons: List<String> = emptyList()
)
