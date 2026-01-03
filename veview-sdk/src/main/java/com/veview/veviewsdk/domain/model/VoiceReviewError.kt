package com.veview.veviewsdk.domain.model

/**
 * Defines specific, typed errors that can occur during the voice review process.
 * This allows the consuming application to programmatically handle different failure scenarios.
 */
enum class VoiceReviewError {
    AUDIO_ANALYSIS_FAILURE,
    CANCELLED,
    AUDIO_CAPTURE_FAILURE,
    UNKNOWN
}
