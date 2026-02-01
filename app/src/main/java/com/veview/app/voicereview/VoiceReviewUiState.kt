package com.veview.app.voicereview

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
sealed interface VoiceReviewUiState {
    @Immutable
    data class Error(@param:StringRes val message: Int) : VoiceReviewUiState

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
    @param:StringRes val status: Int? = null,
    val instructionItems: List<ReviewInstructionItem> = emptyList(),
    val isRecording: Boolean = false,
    val isSpeaking: Boolean = false
)
