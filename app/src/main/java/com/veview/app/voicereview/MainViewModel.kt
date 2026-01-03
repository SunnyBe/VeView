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
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.reviewer.VoiceReviewer
import com.veview.veviewsdk.presentation.VeViewSDK
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class MainViewModel(
    private val voiceReviewer: VoiceReviewer
) : ViewModel() {

    private val _uiState: MutableStateFlow<MainUiState> =
        MutableStateFlow(MainUiState.ReadyToRecord())

    @Suppress("MagicNumber")
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uiStateFlowFromVoiceReviewer().collect { uiState ->
                _uiState.value = uiState
            }
        }
    }

    fun uiStateFlowFromVoiceReviewer(): Flow<MainUiState> {
        return voiceReviewer.state.map { state ->
            when (state) {
                VoiceReviewState.Cancelled -> MainUiState.ReadyToRecord()

                is VoiceReviewState.Error -> MainUiState.Error(state.message)
                VoiceReviewState.Idle -> MainUiState.ReadyToRecord()

                is VoiceReviewState.Initializing -> MainUiState.Recording(
                    status = "Initializing...",
                    isRecording = false
                )

                is VoiceReviewState.Processing -> MainUiState.Recording(
                    status = "Processing...",
                    isRecording = false
                )

                is VoiceReviewState.Recording -> MainUiState.Recording(
                    status = "Recording...",
                    isRecording = state.amplitude > 0
                )

                is VoiceReviewState.Success -> MainUiState.ReviewNoted(result = state.result.summary)
            }
        }
    }

    fun startReview(contextLabel: String) {
        val reviewContext = ReviewContext(UUID.randomUUID().toString(), contextLabel)
        voiceReviewer.start(reviewContext)
    }

    fun pauseReview() {
        voiceReviewer.stop()
    }

    fun cancelReview() {
        voiceReviewer.cancel()
    }

    fun onReviewDone() {
        _uiState.value = MainUiState.ReadyToRecord()
    }

    companion object {
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

                val voiceReviewer = veViewSDK.newAudioReviewer(
                    context = application,
                    config = config
                )
                MainViewModel(voiceReviewer)
            }
        }
    }
}

@Immutable
sealed interface MainUiState {
    @Immutable
    data class Error(val message: String) : MainUiState

    @Immutable
    data class Recording(val status: String?, val isRecording: Boolean = false) : MainUiState

    @Immutable
    data class ReadyToRecord(val input: String? = null) : MainUiState

    @Immutable
    data class ReviewNoted(val result: String) : MainUiState
}
