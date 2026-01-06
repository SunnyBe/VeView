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
import com.veview.veviewsdk.annotations.FieldDescription
import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.model.ReviewContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import java.io.File

/**
 * A generic analysis engine that uses the OpenAI API (Whisper for transcription and a GPT model
 * for analysis) to process an audio file and return a structured response of a client-defined type [T].
 *
 * This engine dynamically constructs a system prompt based on the fields of the provided [responseType]
 * and their associated [FieldDescription] annotations.
 *
 * @param T The target data class for the structured analysis result.
 * @param openAI The configured [OpenAI] client.
 * @param moshi The [Moshi] instance for JSON deserialization.
 * @param responseType The `Class` object of the target data type [T], used for reflection and parsing.
 */
@Suppress("UnusedPrivateProperty")
internal class OpenAIAnalysisEngine<T>(
    private val openAI: OpenAI,
    private val moshi: Moshi,
    private val responseType: Class<T>
) : AnalysisEngine<T> {

    /**
     * Analyzes the audio file by first transcribing it and then passing the transcript to a GPT model
     * for structured analysis, which is then deserialized into the specified type [T].
     *
     * @throws AnalysisFailedException if any step of the process (transcription, analysis, or parsing) fails.
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun analyze(audioFile: File, reviewContext: ReviewContext): T {
        try {
            Timber.tag(LOG_TAG).d("Starting analysis for file: ${audioFile.path}")

            val transcript = transcribe(audioFile)
            Timber.d("Transcription successful: $transcript")

            val analysisJson = getAnalysis(transcript, reviewContext)
            Timber.d("GPT Analysis successful: $analysisJson")

            return parseAnalysisJson(analysisJson, responseType)
        } catch (cause: Exception) {
            Timber.tag(LOG_TAG).e(cause, "Analysis failed: ${cause.message}")
            throw AnalysisFailedException(cause.message ?: "Unknown issue with connection", cause)
        }
    }

    /**
     * Transcribes the given audio file using the OpenAI Whisper API.
     */
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

            Timber.tag(LOG_TAG).d("TRANSCRIPTION RESPONSE: $response")
            return response.text
        } catch (e: OpenAIAPIException) {
            val detailedMessage = "(Code: ${e.error.detail?.code}) ${e.error.detail?.message}"
            Timber.tag(LOG_TAG).e(e, "Transcription failed: $detailedMessage")
            throw AnalysisFailedException(detailedMessage, e)
        }
    }

    /**
     * Submits the transcript to a GPT model for structured analysis based on a dynamically generated prompt.
     */
    private suspend fun getAnalysis(transcript: String, reviewContext: ReviewContext): String {
        try {
            val request = ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo"),
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = createSystemPrompt(reviewContext)
                    ),
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

    /**
     * Parses the JSON response from the GPT model into the specified data type [T].
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun <T> parseAnalysisJson(analysisJson: String, responseType: Class<T>): T {
        try {
            val jsonAdapter = moshi.adapter(responseType)
            return jsonAdapter.fromJson(analysisJson)
                ?: throw IllegalArgumentException("Failed to parse GPT analysis JSON into ${responseType.simpleName}")
        } catch (cause: IllegalArgumentException) {
            throw AnalysisFailedException("Unable to parse response string to object", cause)
        }
    }

    /**
     * Dynamically creates a system prompt for the GPT model based on the fields of the [responseType].
     * It uses reflection to read the field names and their descriptions from the [FieldDescription] annotation.
     */
    private fun createSystemPrompt(reviewContext: ReviewContext): String { // MODIFIED
        val properties = responseType.declaredFields.joinToString(separator = ",\n") { field ->
            val description = field.getAnnotation(FieldDescription::class.java)?.text
                ?: "A value for the '${field.name}' field."
            """  "${field.name}": "($description, must be ${field.type.name.lowercase()})" """
        }

        val contextBlock = """
            The user is reviewing an item or service with the following details:
            - Item/Service Name: "${reviewContext.subject.name ?: "Not specified"}"
            - Country Code: "${reviewContext.locale?.countryCode ?: "Not provided"}"
            - Meta Data: "${reviewContext.metadata}"
        """.trimIndent()

        return """
            You are an expert text analysis system. The user will provide you with a text transcript of a review.
            $contextBlock
    
            Your task is to analyze the text and return ONLY a single, raw JSON object that conforms to the following structure and descriptions.
    
            Here is the JSON structure to follow:
            {
                $properties
            }
    
            Do not include any text, explanation, or markdown formatting before or after the JSON object. Your entire response must be the raw JSON content.
        """.trimIndent()
    }

    companion object {
        private const val LOG_TAG = "OpenAIAnalysisEngine"
    }

    /**
     * An exception thrown when any part of the analysis process fails.
     */
    internal class AnalysisFailedException(message: String, cause: Throwable? = null) :
        Exception(message, cause)
}
