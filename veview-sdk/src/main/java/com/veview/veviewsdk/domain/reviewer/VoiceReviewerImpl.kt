package com.veview.veviewsdk.domain.reviewer

import android.Manifest
import androidx.annotation.RequiresPermission
import com.veview.veviewsdk.data.analysis.AnalysisFailedException
import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.AudioRecordState
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.model.VoiceReviewError
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

internal class VoiceReviewerImpl internal constructor(
    private val configProvider: ConfigProvider,
    private val analysisEngine: AnalysisEngine,
    private val audioProviderFactory: AudioCaptureProvider.Factory,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: DispatcherProvider
) : VoiceReviewer {

    private var sessionJob: Job? = null

    // This will hold the provider for the *current* session only.
    private val sessionAudioProvider = AtomicReference<AudioCaptureProvider?>(null)

    private val _state = MutableStateFlow<VoiceReviewState>(VoiceReviewState.Idle)
    override val state: StateFlow<VoiceReviewState>
        get() = _state.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start(reviewContext: ReviewContext) {
        println("Start Hit: Is session active: ${sessionJob?.isActive}")
        if (sessionJob?.isActive == true) {
            // Active session is running. TODO Record Non-fatal
            Timber.tag(LOG_TAG).w("An active session is in progress.")
            _state.value = VoiceReviewState.Error(
                errorType = VoiceReviewError.RECORDING_FAILED,
                message = "There is an active recording session.",
                throwable = IllegalStateException("There is an active recording session.")
            )
            return
        }

        println("I am here because there is no active session.")

        Timber.tag(LOG_TAG).d("Listening started")
        _state.value = VoiceReviewState.Initializing(reviewContext)

        sessionJob = coroutineScope.launch {
            try {
                val audioCaptureProvider = audioProviderFactory.create(this)
                sessionAudioProvider.set(audioCaptureProvider)

                val config = configProvider.voiceReviewConfigFlow.first()

                audioCaptureProvider.startRecording(
                    generateTimestampFileName(),
                    config,
                    config.recordDuration
                )
                    .collect { audioRecordingState ->
                        handleAudioRecordingState(audioRecordingState)
                    }
                // proceeds to finally only when collection is done or cancelled.
            } catch (cause: CancellationException) {
                Timber.tag(LOG_TAG).d("Session was cancelled")
                throw cause
            } catch (cause: Exception) {
                val errorType = when (cause) {
                    is AnalysisFailedException -> VoiceReviewError.PROCESSING_FAILED
                    else -> VoiceReviewError.UNKNOWN
                }
                _state.value = VoiceReviewState.Error(
                    errorType = errorType,
                    message = "An Unexpected error occurred: ${cause.message}",
                    throwable = cause
                )
                sessionAudioProvider.set(null)
            } finally {
                // This block now correctly runs after the session is over (completed or cancelled).
                Timber.tag(LOG_TAG).d("Session job finished. Cleaning up provider reference.")
                sessionAudioProvider.set(null)
            }
        }
    }

    private suspend fun handleAudioRecordingState(state: AudioRecordState) {
        when (state) {
            is AudioRecordState.DataChunkReady -> {
                val audioData = state.chunk
                Timber.tag(LOG_TAG).d("Audio Byte ${audioData.joinToString(",")}")
                val currentState = _state.value
                if (currentState is VoiceReviewState.Recording) {
                    _state.value = currentState.copy(amplitude = calculateAmplitude(audioData))
                } else {
                    _state.value = VoiceReviewState.Recording(
                        file = state.audioFile,
                        durationMillis = 0,
                        amplitude = 0
                    )
                }
            }

            is AudioRecordState.Done -> {
                _state.value = VoiceReviewState.Processing(state.audioFile)
                val audioFile = state.audioFile
                val result = withContext(coroutineDispatcher.io) {
                    analysisEngine.analyze(audioFile)
                }
                _state.value = VoiceReviewState.Success(result)
            }

            is AudioRecordState.Error -> {
                _state.value = VoiceReviewState.Error(
                    errorType = VoiceReviewError.RECORDING_FAILED,
                    message = "Recording error: ${state.message}",
                    throwable = IllegalStateException(state.message)
                )
            }

            AudioRecordState.Idle -> {
                Timber.tag(LOG_TAG).i("Recording Session is idle.")
            }

            is AudioRecordState.Starting -> {
                Timber.tag(LOG_TAG).i("Recording Session started.")
            }

            is AudioRecordState.Stopped -> {
                Timber.tag(LOG_TAG).i("Recording Session stopped.")
            }

            is AudioRecordState.Started -> {
                Timber.tag(LOG_TAG)
                    .i("Recording Session started at ${state.start}.")
            }
        }
    }

    // Clean up resource
    override fun stop() {
        sessionAudioProvider.get()?.stopRecording()
    }

    override fun cancel() {
        sessionAudioProvider.set(null)
        println("Do I have an active session to cancel? ${sessionJob?.isActive}")
        if (sessionJob?.isActive == true) {
            Timber.tag(LOG_TAG).d("Canceling voice review session.")
            sessionJob?.cancel("Voice review cancelled by user.")
        }
        _state.value = VoiceReviewState.Cancelled
    }

    @Suppress("MagicNumber")
    private fun calculateAmplitude(audioData: ByteArray): Int {
        var sumOfSquares = 0.0
        // We are using 16-bit PCM, so each sample is 2 bytes in little-endian order.
        for (i in audioData.indices step 2) {
            if (i + 1 < audioData.size) {
                // Combine two bytes to form a 16-bit sample
                var sample = (audioData[i + 1].toInt() shl 8) or (audioData[i].toInt() and 0xFF)
                if (sample > 32767) sample -= 65536 // Handle signed 16-bit overflow
                sumOfSquares += (sample * sample).toDouble()
            }
        }
        val rms = sqrt(sumOfSquares / (audioData.size / 2))
        // Scale the RMS value to a more usable range, e.g., 0-100.
        // The max RMS for 16-bit audio is ~32767. This scaling is arbitrary and can be tuned.
        return (rms / 32767.0 * 100).toInt()
    }

    private fun generateTimestampFileName(): String {
        return "review_${System.currentTimeMillis()}.wav"
    }

    companion object {
        private const val LOG_TAG = "VoiceReviewerImpl"
    }
}
