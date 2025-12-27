package com.veview.veviewsdk.domain.reviewer

import android.Manifest
import android.accounts.AuthenticatorException
import androidx.annotation.RequiresPermission
import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.model.VoiceReviewError
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private val apiKey: String,
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
        if (sessionJob?.isActive == true) {
            // Active session is running. TODO Record Non-fatal
            Timber.tag(LOG_TAG).w("Start called while a review session is already in progress.")
            return
        }
        Timber.tag(LOG_TAG).d("Listening started")

        sessionJob = coroutineScope.launch {
            try {
                val audioCaptureProvider = audioProviderFactory.create(this) // use session job
                sessionAudioProvider.set(audioCaptureProvider)

                val config = configProvider.voiceReviewConfigFlow.first()
                _state.value = VoiceReviewState.Initializing(reviewContext)

                authChecks(apiKey = apiKey)

                launch {
                    audioCaptureProvider.audioStream.collect { audioData ->
                        Timber.tag(LOG_TAG).d("Audio Byte ${audioData.joinToString(",")}")
                        val amplitude = calculateAmplitude(audioData)
                        val currentState = _state.value
                        if (currentState is VoiceReviewState.Recording) {
                            _state.value = currentState.copy(amplitude = amplitude)
                        }
                    }
                }

                val audioFile = audioCaptureProvider.startRecording(
                    generateTimestampFileName(),
                    config
                )
                _state.value = VoiceReviewState.Recording(audioFile, 0L, 0)

                delay(config.recordDuration) // Duration of recording

                sessionAudioProvider.get()?.stopRecording()
                _state.value = VoiceReviewState.Processing(audioFile)

                val result = analysisEngine.analyze(audioFile)
                _state.value = VoiceReviewState.Success(result)
            } catch (cause: AuthenticatorException) {
                _state.value = VoiceReviewState.Error(
                    errorType = VoiceReviewError.INVALID_API_KEY,
                    message = "Failed: ${cause.message}",
                    throwable = cause
                )
            } catch (cause: CancellationException) {
                Timber.tag(LOG_TAG).d("Session cancelled as expected. Stop pending jobs")
                throw cause
            } catch (cause: Exception) {
                _state.value = VoiceReviewState.Error(
                    errorType = VoiceReviewError.UNKNOWN,
                    message = "An Unexpected error occurred: ${cause.message}",
                    throwable = cause
                )
            } finally {
                sessionAudioProvider.set(null)
            }
        }
    }

    // Clean up resource
    override fun stop() {
        sessionAudioProvider.get()?.stopRecording()
    }

    override fun cancel() {
        if (sessionJob?.isActive == true) {
            Timber.tag(LOG_TAG).d("Canceling voice review session.")
            sessionJob?.cancel("Voice review cancelled by user.")
        }
        _state.value = VoiceReviewState.Cancelled
    }

    private suspend fun authChecks(apiKey: String?) = withContext(coroutineDispatcher.io) {
        if (apiKey.isNullOrBlank()) throw AuthenticatorException("API key is null or blank")
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
