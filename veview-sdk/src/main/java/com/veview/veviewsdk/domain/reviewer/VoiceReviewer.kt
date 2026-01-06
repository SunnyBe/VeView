package com.veview.veviewsdk.domain.reviewer

import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides a comprehensive interface for managing a voice review process that results in a
 * structured analysis of a specific type [T].
 *
 * @param T The custom data type for the structured analysis result.
 */
interface VoiceReviewer<T> {

    /**
     * A [StateFlow] that emits the current [VoiceReviewState] of the voice review process.
     * The success state will contain a result of type [T]. This allows clients to observe
     * the entire lifecycle of a voice review in a type-safe manner.
     */
    val state: StateFlow<VoiceReviewState<T>>

    /**
     * Starts the voice review process.
     *
     * @param reviewContext The context for the review, including a unique reference ID and other metadata.
     */
    fun start(reviewContext: ReviewContext)

    /**
     * Stops the voice recording. The recorded audio will then be sent for analysis.
     */
    fun stop()

    /**
     * Cancels the voice review process at any stage.
     * This will stop any active recording and halt any ongoing analysis.
     */
    fun cancel()
}
