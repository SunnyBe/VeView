package com.veview.veviewsdk.data.analysis

import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.squareup.moshi.Moshi
import com.veview.veviewsdk.data.analysis.network.GptAnalysisResponse
import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.model.VoiceReview
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import java.io.File

/**
 * Implementation using OpenAI's Whisper + GPT.
 */
@Suppress("UnusedPrivateProperty")
internal class OpenAIAnalysisEngine(
    private val openAI: OpenAI,
    private val moshi: Moshi
) : AnalysisEngine {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun analyze(audioFile: File): VoiceReview {
        try {
            Timber.tag(LOG_TAG).d("Starting analysis for file: ${audioFile.path}")

            val transcript = transcribe(audioFile)
            Timber.d("Transcription successful: $transcript")

            val analysisJson = getAnalysis(transcript)
            Timber.d("GPT Analysis successful: $analysisJson")

            val parsedAnalysis = parseAnalysisJson(analysisJson)

            return VoiceReview(
                transcript = transcript,
                summary = parsedAnalysis.summary,
                estimatedRating = parsedAnalysis.estimatedRating,
                pros = parsedAnalysis.pros,
                cons = parsedAnalysis.cons,
                audioFile = audioFile
            )
        } catch (cause: Exception) {
            Timber.tag(LOG_TAG).e(cause, "Analysis failed: ${cause.message}")
            throw AnalysisFailedException(cause.message ?: "Unknown issue with connection", cause)
        }
    }

    private suspend fun transcribe(audioFile: File): String {
        try {
            val request = TranscriptionRequest(
                audio = FileSource(
                    name = audioFile.name,
                    source = FileSystem.SYSTEM.source(audioFile.toOkioPath())
                ),
                model = ModelId("whisper-1")
            )
            val response = openAI.transcription(request)

            return response.text
        } catch (e: OpenAIAPIException) {
            val detailedMessage = "(Code: ${e.error.detail?.code}) ${e.error.detail?.message}"
            Timber.tag(LOG_TAG).e(e, "Transcription failed: $detailedMessage")
            throw AnalysisFailedException(detailedMessage, e)
        }
    }

    private suspend fun getAnalysis(transcript: String): String {
        try {
            val request = ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo"),
                messages = listOf(
                    ChatMessage(role = ChatRole.System, content = createSystemPrompt()),
                    ChatMessage(role = ChatRole.User, content = transcript)
                )
            )

            val chatCompletion = openAI.chatCompletion(request)

            return chatCompletion.choices.firstOrNull()?.message?.content
                ?: throw AnalysisFailedException("GPT response contained no choices")
        } catch (e: OpenAIAPIException) {
            Timber.tag(LOG_TAG).e(e, "OpenAI API error during transcription: ${e.message}")
            val detailedMessage =
                "Transcription failed with API error: ${e.error.detail?.message} (Code: ${e.error.detail?.code})"
            throw AnalysisFailedException(detailedMessage, e)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseAnalysisJson(analysisJson: String): GptAnalysisResponse {
        try {
            val jsonAdapter = moshi.adapter(GptAnalysisResponse::class.java)
            val parsedAnalysis = jsonAdapter.fromJson(analysisJson)
                ?: throw IllegalArgumentException("Failed to parse GPT analysis JSON")
            return parsedAnalysis
        } catch (cause: IllegalArgumentException) {
            throw AnalysisFailedException("Unable to parse response string to object", cause)
        }
    }

    // Move this to config.json for business wide customization
    private fun createSystemPrompt(): String {
        // This prompt remains unchanged and is critically important.
        return """
            You are a review analysis expert. The user will provide you with a text transcript of a voice review.
            Your task is to analyze the text and return ONLY a JSON object with the following structure:
            {
              "summary": "A one-sentence summary of the review.",
              "rating": an integer from 1 to 5, where 1 is very negative and 5 is very positive,
              "pros": ["A list of positive points mentioned.", "Another positive point."],
              "cons": ["A list of negative points mentioned."]
            }
            Do not include any text, explanation, or markdown formatting before or after the JSON object.
        """.trimIndent()
    }

    companion object {
        private const val LOG_TAG = "OpenAIAnalysisEngine"
    }

    internal class AnalysisFailedException(message: String, cause: Throwable? = null) :
        Exception(message, cause)
}
