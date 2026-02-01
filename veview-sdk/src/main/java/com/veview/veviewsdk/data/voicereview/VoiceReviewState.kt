package com.veview.veviewsdk.data.voicereview

import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.model.VoiceReviewError
import java.io.File

/**
 * Represents the various states of the voice review lifecycle. This is a sealed interface,
 * providing a restricted class hierarchy for managing state in a type-safe way.
 *
 * @param T The type of the successful analysis result.
 */
sealed interface VoiceReviewState<out T> {
    /**
     * The initial state before any review has started, or the final state after a review has
     * been completed or cancelled. The system is ready for a new review.
     */
    object Idle : VoiceReviewState<Nothing>

    /**
     * The system is initializing for a new voice review. This may include requesting permissions
     * or performing initial checks before recording begins.
     *
     * @param context The context for the review being initiated.
     */
    data class Initializing(val context: ReviewContext) : VoiceReviewState<Nothing>

    /**
     * The system is actively recording audio. This state provides feedback on the recording process.
     *
     * @param file The file where the recorded audio is being stored.
     * @param durationMillis The elapsed duration of the recording in milliseconds.
     * @param amplitude The most recent audio amplitude, useful for UI visualizations.
     */
    data class Recording(val file: File?, val durationMillis: Long, val amplitude: Int) :
        VoiceReviewState<Nothing>

    /**
     * The audio recording is complete, and the system is processing the data.
     * This includes transcription and analysis.
     *
     * @param file The completed audio file that is being processed.
     */
    data class Processing(val file: File) : VoiceReviewState<Nothing>

    /**
     * The voice review was successfully processed, and the structured analysis result is available.
     * This is a terminal success state.
     *
     * @param T The type of the result data.
     * @param result The successfully processed voice review data of type [T].
     */
    data class Success<T>(val result: T) : VoiceReviewState<T>

    /**
     * An error occurred during the voice review lifecycle. This state provides details about the failure.
     *
     * @param errorType A specific [VoiceReviewError] to allow for granular error handling.
     * @param message A developer-facing message describing the error.
     * @param throwable The exception that was thrown, useful for debugging.
     */
    data class Error(
        val errorType: VoiceReviewError,
        val message: String,
        val throwable: Throwable? = null
    ) : VoiceReviewState<Nothing>

    /**
     * The voice review process was explicitly cancelled by the user.
     * This is a terminal state.
     */
    object Cancelled : VoiceReviewState<Nothing>
}
