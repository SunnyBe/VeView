package com.veview.veviewsdk.data.audiocapture

import android.Manifest
import android.content.Context
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.data.coroutine.DefaultDispatcherProvider
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.AudioRecordState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock.System
import kotlin.time.Duration

/**
 * An Android-specific, stateless service for capturing raw audio from the microphone
 * and saving it as a valid WAV file.
 *
 * This class is designed to be robust and thread-safe. Its core functionality is
 * validated via instrumentation tests.
 *
 * @property context The Android context, preferably from a foreground service for background recording.
 * @property scope The CoroutineScope that will manage the lifecycle of the recording job.
 * @property dispatcherProvider Provides dispatchers for I/O and other tasks.
 */
private const val LOG_TAG = "AudioCaptureProvider"

internal class AndroidAudioCaptureProvider(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider
) : AudioCaptureProvider {
    private var audioRecord: AudioRecord? = null

    private var isRecording = AtomicBoolean(false)
    private var recordingFile: File? = null // Keep track of the file

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording(
        fileName: String,
        config: VoiceReviewConfig,
        duration: Duration
    ): Flow<AudioRecordState> = flow {
        Timber.tag(LOG_TAG).d("Current Config: $config")
        check(isRecording.compareAndSet(false, true)) { "There is an ongoing recording." }

        emit(AudioRecordState.Starting) // Emit starting state

        val outputFile = generateOutputFile(config, fileName)

        val minBufferSize = AudioRecord.getMinBufferSize(
            config.sampleRate,
            config.channelConfig,
            config.audioFormat
        )
        val bufferSize = minBufferSize * 2
        check(bufferSize > 0) { "Invalid buffer size calculated: $bufferSize" }

        audioRecord = createAudioRecordInstance(config, bufferSize)

        val fileOutputStream = FileOutputStream(outputFile, true)
        val audioBuffer = ByteArray(bufferSize)

        audioRecord?.startRecording()
        val startTime = System.now() // Note start time
        emit(AudioRecordState.Started(outputFile, startTime))
        Timber.tag(LOG_TAG).d("Started audio recording at $startTime.")

        try {
            // Waiting for audio data and emitting chunks
            while (scope.isActive && isRecording.get() &&
                (System.now() - startTime) < duration
            ) {
                val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                if (readSize > 0) {
                    val validData = audioBuffer.copyOf(readSize)
                    val emissionTime = System.now()
                    emit(
                        AudioRecordState.DataChunkReady(
                            outputFile,
                            validData,
                            emissionTime - startTime
                        )
                    )
                    fileOutputStream.write(validData)
                }
            }
        } finally {
            Timber.d("Audio recording loop completed at ${System.now()}. Cleaning up resources.")
            isRecording.set(false)
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            fileOutputStream.close()
        }

        emit(AudioRecordState.Stopped(System.now()))

        recordingFile?.let {
            WavFileUtil.updateWavHeader(it)
            Timber.d("WAV header updated for file: ${it.absolutePath}")
            emit(AudioRecordState.Done(it))
        }
    }.catch { cause ->
        Timber.e(cause, "Error in audio recording flow.")
        isRecording.set(false)
        if (cause !is CancellationException) {
            val exception =
                AudioRecordingException(cause.message, cause)
            emit(AudioRecordState.Error(exception))
        } else {
            throw cause // Stop subsequent processing on cancellation
        }
    }
        .flowOn(dispatcherProvider.default)

    /**
     * Signals the recording loop to stop. The loop will then enter its `finally`
     * block to handle all cleanup and file finalization.
     */
    override fun stopRecording() {
        if (isRecording.compareAndSet(true, false)) {
            Timber.tag(LOG_TAG).d("Stopping recording...")
        }
    }

    override fun cancel() {
        Timber.tag(LOG_TAG).d("Cancelling audio capture job.")
        stopRecording()
    }

    private suspend fun generateOutputFile(config: VoiceReviewConfig, fileName: String): File =
        withContext(dispatcherProvider.io) {
            val file = File(config.storageDirectory ?: context.cacheDir, fileName)
            recordingFile = file
            Timber.d("Recording file created at: ${file.absolutePath}")
            WavFileUtil.writeWavHeader(
                file,
                config.sampleRate,
                config.numChannels,
                config.bitsPerSample
            )
            Timber.d("WAV header written to file: ${file.absolutePath}")
            file
        }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecordInstance(
        config: VoiceReviewConfig,
        bufferSize: Int
    ): AudioRecord {
        return AudioRecord(
            config.audioSource,
            config.sampleRate,
            config.channelConfig,
            config.audioFormat,
            bufferSize
        )
            .also {
                check(it.state == AudioRecord.STATE_INITIALIZED) {
                    "AudioRecord init failed. Permissions or hardware issues."
                }
            }
    }

    companion object : AudioCaptureProvider.Factory {
        private lateinit var appContext: Context
        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        override fun create(scope: CoroutineScope): AudioCaptureProvider {
            return AndroidAudioCaptureProvider(
                context = appContext,
                scope = scope,
                dispatcherProvider = DefaultDispatcherProvider
            )
        }
    }

    internal class AudioRecordingException(message: String?, cause: Throwable? = null) :
        Exception(message, cause)
}
