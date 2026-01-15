package com.veview.app.voicereview

import android.media.AudioFormat
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.veview.app.BuildConfig
import com.veview.app.MainApplication
import com.veview.app.R
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.model.SubjectType
import com.veview.veviewsdk.domain.model.VoiceReviewError
import com.veview.veviewsdk.domain.reviewer.VoiceReviewer
import com.veview.veviewsdk.presentation.VeViewSDK
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class MainViewModel(
    private val voiceReviewer: VoiceReviewer<MainReviewResponse>
) : ViewModel() {

    private val _effects: MutableSharedFlow<VoiceReviewEffect> = MutableSharedFlow()
    val effects: SharedFlow<VoiceReviewEffect> = _effects

    private val _uiState: MutableStateFlow<MainUiState> =
        MutableStateFlow(MainUiState())

    @Suppress("MagicNumber")
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            voiceReviewer.state.collect { state ->
                val voiceReviewState = when (state) {
                    VoiceReviewState.Cancelled -> {
                        VoiceReviewUiState.VoiceReviewState(
                            recordingState = RecordingState(
                                status = R.string.recording_cancelled_label,
                                isRecording = false
                            )
                        )
                    }

                    is VoiceReviewState.Error -> {
                        Timber.tag(LOG_TAG).d("Review error: ${state.errorType} - ${state.message}")
                        val errorMessage = when (state.errorType) {
                            VoiceReviewError.AUDIO_CAPTURE_FAILURE -> R.string.audio_capture_failure_message
                            VoiceReviewError.AUDIO_ANALYSIS_FAILURE -> R.string.analysis_failure_message
                            else -> R.string.unknown_error_message
                        }
                        VoiceReviewUiState.Error(errorMessage)
                    }

                    VoiceReviewState.Idle -> {
                        VoiceReviewUiState.VoiceReviewState()
                    }

                    is VoiceReviewState.Initializing -> {
                        VoiceReviewUiState.VoiceReviewState(
                            recordingState = RecordingState(
                                status = R.string.initializing_label,
                                isRecording = true
                            )
                        )
                    }

                    is VoiceReviewState.Processing -> {
                        VoiceReviewUiState.VoiceReviewState(
                            recordingState = RecordingState(
                                status = R.string.recording_label,
                                isRecording = true
                            )
                        )
                    }

                    is VoiceReviewState.Recording -> {
                        VoiceReviewUiState.VoiceReviewState(
                            recordingState = RecordingState(
                                status = R.string.recording_label,
                                isRecording = true,
                                isSpeaking = state.amplitude > 0
                            )
                        )
                    }

                    is VoiceReviewState.Success -> VoiceReviewUiState.ReviewNoted(result = state.result.summary)
                }

                _uiState.update { it.copy(voiceReviewState = voiceReviewState) }
            }
        }
    }

    fun handleEvent(event: VoiceReviewEvent) {
        when (event) {
            is VoiceReviewEvent.StartRecording -> { startReview(event.businessName) }
            VoiceReviewEvent.PauseRecording -> pauseReview()
            VoiceReviewEvent.CancelRecording -> cancelReview()
            VoiceReviewEvent.ConfirmCancel -> onExitRequested(ExitReason.USER_TRIGGER)
            VoiceReviewEvent.DoneRecording -> onReviewDone()
        }
    }

    private fun startReview(businessName: String) {
        val reviewContext = ReviewContext(
            UUID.randomUUID().toString(),
            ReviewContext.ReviewSubject(businessName, SubjectType.RESTAURANT)
        )
        Timber.tag(LOG_TAG).d("Starting review for business: $businessName")
        voiceReviewer.start(reviewContext)
    }

    private fun pauseReview() {
        Timber.tag(LOG_TAG).d("Pausing review from ViewModel.")
        voiceReviewer.stop()
    }

    private fun cancelReview() {
        Timber.tag(LOG_TAG).d("Cancelling review from ViewModel.")
        voiceReviewer.cancel()
    }

    private fun onReviewDone() {
        Timber.tag(LOG_TAG).d("Review done acknowledged by user.")
        _uiState.update { it.copy(voiceReviewState = VoiceReviewUiState.VoiceReviewState()) }
    }

    fun onPermissionGranted() {
        Timber.tag(LOG_TAG).d("Permission granted.")
        _uiState.update { it.copy(isPermissionEnabled = true) }
    }

    fun onPermissionDenied() {
        Timber.tag(LOG_TAG).d("Permission denied.")
        _uiState.update { it.copy(isPermissionEnabled = false) }
        viewModelScope.launch {
            _effects.emit(VoiceReviewEffect.CloseApplication(ExitReason.PERMISSION_DENIED))
        }
    }

    fun onExitRequested(reason: ExitReason) {
        viewModelScope.launch {
            _effects.emit(VoiceReviewEffect.CloseApplication(reason))
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceReviewer.cancel()
    }

    companion object {
        private const val LOG_TAG = "MainViewModel"

        @Suppress("MagicNumber")
        val FACTORY = viewModelFactory {
            initializer {
                val veViewSDK = VeViewSDK.Builder(
                    apiKey = BuildConfig.VEVIEW_API_KEY,
                    isDebug = BuildConfig.DEBUG
                ).build()

                val config = VoiceReviewConfig.Builder()
                    .setRecordDuration(1.minutes)
                    .setSampleRate(16000)
                    .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication

                val voiceReviewer = veViewSDK.newCustomAudioReviewer(
                    context = application,
                    responseType = MainReviewResponse::class.java,
                    config = config
                )
                MainViewModel(voiceReviewer)
            }
        }
    }
}

@Immutable
data class MainUiState(
    val isPermissionEnabled: Boolean = false,
    val voiceReviewState: VoiceReviewUiState = VoiceReviewUiState.VoiceReviewState()
)
