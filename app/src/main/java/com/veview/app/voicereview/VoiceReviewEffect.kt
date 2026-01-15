package com.veview.app.voicereview

sealed interface VoiceReviewEffect {
    data class CloseApplication(val reason: ExitReason = ExitReason.USER_TRIGGER) :
        VoiceReviewEffect
}
