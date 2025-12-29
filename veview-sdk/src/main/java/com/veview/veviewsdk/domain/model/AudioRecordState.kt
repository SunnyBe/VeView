package com.veview.veviewsdk.domain.model

import java.io.File
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface AudioRecordState {
    data object Idle : AudioRecordState
    data object Starting : AudioRecordState // Init
    data class Started(val audioFile: File, val start: Instant) :
        AudioRecordState // Recording started

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

    data class Stopped(val stop: Instant) : AudioRecordState // Recording stopped
    data class Done(val audioFile: File) : AudioRecordState // Recording done
    data class Error(val message: String) : AudioRecordState // Error occurred
}
