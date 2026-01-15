package com.veview.app.voicereview

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
sealed interface VoiceReviewUiState {
    @Immutable
    data class Error(@StringRes val message: Int) : VoiceReviewUiState

    @Immutable
    data class VoiceReviewState(
        val recordingState: RecordingState = RecordingState(),
        val warning: String? = null
    ) : VoiceReviewUiState

    @Immutable
    data class ReviewNoted(val result: String) : VoiceReviewUiState
}

@Immutable
data class RecordingState(
    @StringRes val status: Int? = null,
    val isRecording: Boolean = false,
    val isSpeaking: Boolean = false
)
