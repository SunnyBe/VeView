package com.veview.veviewsdk.domain.model

import java.io.File
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Represents the different states of the audio recording process.
 * This sealed interface provides a restricted hierarchy for managing the recording state in a type-safe manner.
 */
sealed interface AudioRecordState {
    /** The recorder is idle and not currently recording. */
    data object Idle : AudioRecordState

    /** The recording is being initialized but has not yet started. */
    data object Starting : AudioRecordState

    /**
     * The recording has successfully started.
     *
     * @param audioFile The file where the audio is being saved.
     * @param start The timestamp when the recording began.
     */
    data class Started(val audioFile: File, val start: Instant) : AudioRecordState

    /**
     * A new chunk of audio data has been captured.
     *
     * @param audioFile The file where the audio is being saved.
     * @param chunk The raw byte array of the latest audio data.
     * @param durationSoFar The total duration of the recording up to this point.
     */
    data class DataChunkReady(
        val audioFile: File,
        val chunk: ByteArray,
        val durationSoFar: Duration
    ) : AudioRecordState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DataChunkReady

            if (!chunk.contentEquals(other.chunk)) return false
            if (durationSoFar != other.durationSoFar) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chunk.contentHashCode()
            result = 31 * result + durationSoFar.hashCode()
            return result
        }
    }

    /**
     * The recording has been stopped.
     *
     * @param stop The timestamp when the recording was stopped.
     */
    data class Stopped(val stop: Instant) : AudioRecordState

    /**
     * The recording is complete and the audio file is finalized.
     *
     * @param audioFile The final, saved audio file.
     */
    data class Done(val audioFile: File) : AudioRecordState

    /**
     * An error occurred during the recording process.
     *
     * @param exception The exception that was thrown.
     */
    data class Error(val exception: Exception) : AudioRecordState
}
