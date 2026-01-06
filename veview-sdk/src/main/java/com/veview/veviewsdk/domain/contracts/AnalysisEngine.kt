package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.domain.model.ReviewContext
import java.io.File

/**
 * A generic interface for analyzing an audio file and returning a structured response of type [T].
 * This contract decouples the core review logic from the specific implementation of the analysis engine,
 * allowing for different strategies (e.g., OpenAI, local models).
 *
 * @param T The data type of the structured analysis result.
 */
interface AnalysisEngine<T> {

    /**
     * Analyzes the given audio file using the provided context and returns a structured result.
     *
     * @param audioFile The audio file to be analyzed.
     * @param reviewContext The context of the review, which may contain metadata or prompts
     *   to guide the analysis.
     * @return A structured result of type [T].
     */
    suspend fun analyze(audioFile: File, reviewContext: ReviewContext): T
}
