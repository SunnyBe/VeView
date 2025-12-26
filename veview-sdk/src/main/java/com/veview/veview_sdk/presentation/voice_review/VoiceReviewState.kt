package com.veview.veview_sdk.presentation.voice_review

import com.veview.veview_sdk.domain.model.ReviewContext
import com.veview.veview_sdk.domain.model.VoiceReview
import com.veview.veview_sdk.domain.model.VoiceReviewError
import java.io.File

/**
 * Represents the various states of the voice review lifecycle. This sealed interface
 * provides a restricted class hierarchy for managing the state in a type-safe way.
 */
sealed interface VoiceReviewState {
    /**
     * The initial state or the state after a review process has completed or been cancelled.
     * The system is ready to start a new voice review.
     */
    object Idle : VoiceReviewState

    /**
     * The system is preparing for a voice review. This state includes requesting necessary
     * permissions (like microphone access) and performing any authentication checks.
     * This is an intermediate state before recording begins.
     *
     * @param context The context for the review being initiated.
     */
    data class Initializing(val context: ReviewContext) : VoiceReviewState

    /**
     * The system is actively recording audio from the microphone but has not yet
     * started the transcription process. This state is useful for providing early
     * feedback to the user that audio is being captured.
     *
     * @param file The file where the recorded audio is being stored.
     * @param durationMillis The current duration of the recording in milliseconds.
     * @param amplitude The latest audio amplitude, useful for UI feedback.
     */
    data class Recording(val file: File?, val durationMillis: Long, val amplitude: Int) :
        VoiceReviewState

    /**
     * Audio recording is complete, and the system is now processing the data. This includes
     * transcription, and may also involve uploading the audio file to a server.
     *
     * @param file The audio file that was recorded.
     */
    data class Processing(val file: File) : VoiceReviewState

    /**
     * The voice review has been successfully processed, and the resulting data is available.
     * This is a terminal success state.
     *
     * @param result The successfully processed voice review data.
     */
    data class Success(val result: VoiceReview) : VoiceReviewState

    /**
     * An error occurred at some point during the voice review lifecycle.
     * This state provides details about the error.
     *
     * @param errorType A specific error type to allow for more granular error handling.
     * @param message A developer-facing message describing the error.
     * @param userFriendlyMessage An optional message that is safe to display to the user.
     * @param throwable The exception that was thrown, useful for debugging.
     */
    data class Error(
        val errorType: VoiceReviewError,
        val message: String,
        val userFriendlyMessage: String? = null,
        val throwable: Throwable? = null
    ) : VoiceReviewState

    /**
     * The voice review process was explicitly cancelled by the user.
     */
    object Cancelled : VoiceReviewState
}