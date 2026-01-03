package com.veview.veviewsdk.domain.reviewer

import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides a comprehensive interface for managing the voice review process, including the state of the review.
 */
interface VoiceReviewer {

    /**
     * A [StateFlow] that emits the current state of the voice review process.
     * This allows clients to observe the entire lifecycle of a voice review, from recording to completion.
     */
    val state: StateFlow<VoiceReviewState>

    /**
     * Starts the voice review process.
     *
     * @param reviewContext The context of the review, including a unique identifier.
     */
    fun start(reviewContext: ReviewContext)

    /**
     * Stops the voice recording.
     * The recorded audio will then be processed.
     */
    fun stop()

    /**
     * Cancels the voice review process.
     * This will stop the recording and any ongoing processing.
     */
    fun cancel()
}
