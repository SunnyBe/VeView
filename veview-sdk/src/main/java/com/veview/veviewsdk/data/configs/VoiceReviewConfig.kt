package com.veview.veviewsdk.data.configs

import android.media.AudioFormat
import android.media.MediaRecorder
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class VoiceReviewConfig(
    val recordDuration: Duration,
    val storageDirectory: File?,
    val audioFormat: Int,
    val audioSource: Int,
    val sampleRate: Int,
    val channelConfig: Int,
    val bitsPerSample: Int,
    val numChannels: Int
) {

    class Builder {
        private var recordDuration: Duration = DEFAULT_DURATION_MINUTE.minutes
        private var customStorageDir: File? = null
        private var audioFormat: Int = DEFAULT_AUDIO_FORMAT
        private var audioSource: Int = DEFAULT_AUDIO_SOURCE
        private var sampleRate: Int = DEFAULT_SAMPLE_RATE
        private var channelConfig: Int = DEFAULT_CHANNEL_CONFIG
        private var bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE
        private var numChannels: Int = DEFAULT_NUM_CHANNELS

        /**
         * Sets the maximum duration for an audio recording.
         * Defaults to a [DEFAULT_DURATION] if not set.
         */
        fun setRecordDuration(duration: Duration) = apply { this.recordDuration = duration }

        /**
         * Sets a custom directory where audio recordings will be stored.
         * If not set, the SDK will use the app's internal cache directory.
         *
         * @param directory The directory to store files. It must be a writable directory.
         */
        fun setStorageDirectory(directory: File) = apply { this.customStorageDir = directory }

        fun setAudioFormat(format: Int) = apply { this.audioFormat = format }

        fun setChannelConfig(config: Int) = apply { this.channelConfig = config }

        fun setSampleRate(sampleRate: Int) = apply { this.sampleRate = sampleRate }

        fun build(): VoiceReviewConfig {
            require(recordDuration.isPositive()) { "Record duration must be positive" }
            customStorageDir?.let {
                require(it.isDirectory && it.canWrite()) {
                    "An invalid directory or write access is missing"
                }
            }

            return VoiceReviewConfig(
                recordDuration = recordDuration,
                audioFormat = audioFormat,
                storageDirectory = customStorageDir,
                audioSource = audioSource,
                sampleRate = sampleRate,
                channelConfig = channelConfig,
                bitsPerSample = bitsPerSample,
                numChannels = numChannels
            )
        }
    }

    companion object {
        private const val DEFAULT_DURATION_MINUTE = 3
        private const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val DEFAULT_SAMPLE_RATE = 16000
        private const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val DEFAULT_BITS_PER_SAMPLE = 16 // 16-bits for ENCODING_PCM_16BIT
        private const val DEFAULT_NUM_CHANNELS = 1 // for CHANNEL_IN_MONO
    }
}
