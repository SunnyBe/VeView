package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.model.AudioRecordState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * A contract for capturing audio from the device's microphone.
 * This interface abstracts the underlying implementation of the audio recording, allowing for
 * different strategies (e.g., `AudioRecord` on Android) and easier testing.
 */
interface AudioCaptureProvider {

    /**
     * Starts the audio recording and returns a [Flow] of [AudioRecordState]s.
     *
     * @param fileName The name of the file to save the recording to.
     * @param config The [VoiceReviewConfig] to use for this recording session.
     * @param duration The maximum duration of the recording.
     * @return A [Flow] that emits the current state of the recording process.
     */
    fun startRecording(
        fileName: String,
        config: VoiceReviewConfig,
        duration: Duration
    ): Flow<AudioRecordState>

    /**
     * Stops the current recording.
     */
    fun stopRecording()

    /**
     * Cancels the current recording session, discarding any captured data.
     */
    fun cancel()

    /**
     * A factory for creating instances of [AudioCaptureProvider].
     */
    interface Factory {
        /**
         * Creates a new [AudioCaptureProvider] instance within the given [CoroutineScope].
         *
         * @param scope The coroutine scope to use for the recording process.
         */
        fun create(scope: CoroutineScope): AudioCaptureProvider
    }
}
