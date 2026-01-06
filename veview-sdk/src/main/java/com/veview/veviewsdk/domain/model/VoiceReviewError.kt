package com.veview.veviewsdk.domain.model

/**
 * Defines specific, typed errors that can occur during the voice review process.
 * This allows the client application to programmatically handle different failure scenarios.
 */
enum class VoiceReviewError {
    /**
     * An error occurred during the analysis phase after the audio was successfully recorded.
     * This could be due to a network issue, an invalid API key, or a problem with the analysis service.
     */
    AUDIO_ANALYSIS_FAILURE,

    /**
     * The voice review process was explicitly cancelled by the user.
     */
    CANCELLED,

    /**
     * A failure occurred during the audio recording process itself.
     * This could be caused by a lack of permissions, a hardware issue, or an invalid recording configuration.
     */
    AUDIO_CAPTURE_FAILURE,

    /**
     * An unexpected or unknown error occurred that does not fit into the other categories.
     */
    UNKNOWN
}
