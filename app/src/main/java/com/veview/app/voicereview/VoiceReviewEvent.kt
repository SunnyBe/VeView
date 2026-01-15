package com.veview.app.voicereview

sealed interface VoiceReviewEvent {
    data class StartRecording(val businessName: String) : VoiceReviewEvent
    data object PauseRecording : VoiceReviewEvent
    data object CancelRecording : VoiceReviewEvent
    data object ConfirmCancel : VoiceReviewEvent
    data object DoneRecording : VoiceReviewEvent
}

enum class ExitReason {
    USER_TRIGGER,
    PERMISSION_DENIED
}
