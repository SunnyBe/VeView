package com.veview.veviewsdk.domain.model

/**
 * Defines specific, typed errors that can occur during the voice review process.
 * This allows the consuming application to programmatically handle different failure scenarios.
 */
enum class VoiceReviewError {
    AUTHENTICATION_FAILURE,
    PERMISSION_MICROPHONE_DENIED,
    TRANSCRIPTION_FAILED,
    PROCESSING_FAILED,
    NETWORK_ERROR,
    CANCELLED,
    RECORDING_FAILED,
    UNKNOWN
}
