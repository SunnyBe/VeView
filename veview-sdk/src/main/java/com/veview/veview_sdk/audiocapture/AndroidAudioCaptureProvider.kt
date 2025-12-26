package com.veview.veview_sdk.audiocapture

import android.content.Context
import android.media.AudioRecord
import com.veview.veview_sdk.configs.VoiceReviewConfig
import com.veview.veview_sdk.coroutine.DispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

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
internal class AndroidAudioCaptureProvider constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : AudioCaptureProvider {
    private val _audioStream = MutableSharedFlow<ByteArray>()
    override val audioStream: Flow<ByteArray> = _audioStream.asSharedFlow()

    private var audioRecord: AudioRecord? = null
    private var isRecording = AtomicBoolean(false)
    private var recordingFile: File? = null // Keep track of the file

    override suspend fun startRecording(fileName: String, config: VoiceReviewConfig): Pair<File, Job> {
        Timber.tag(LOG_TAG).d("Current Config: $config")

        if (!isRecording.compareAndSet(false, true)) {
            throw IllegalStateException("There is an ongoing recording.")
        }

        // This deferred is used to synchronize the caller with the background recording thread.
        // The `startRecording` function will not return until this signal is completed.
        val recordingStartedSignal = CompletableDeferred<Unit>()

        val outputFile = withContext(dispatcherProvider.io) {
            val file = File(config.storageDirectory ?: context.cacheDir, fileName)
            recordingFile = file

            WavFileUtil.writeWavHeader(
                file,
                config.sampleRate,
                config.numChannels,
                config.bitsPerSample
            )
            file
        }

        val recordingJob = scope.launch(dispatcherProvider.io) {
            // This reference is kept local to the coroutine to ensure it's always handled correctly.
            var fileOutputStream: FileOutputStream? = null
            try {
                // Use a larger buffer for device compatibility
                val minBufferSize = AudioRecord.getMinBufferSize(
                    config.sampleRate,
                    config.channelConfig,
                    config.audioFormat
                )
                // Use a buffer larger than the minimum for better compatibility across devices.
                val bufferSize = minBufferSize * 2

                if (bufferSize <= 0) {
                    throw IllegalStateException("Invalid buffer size calculated: $bufferSize")
                }

                audioRecord =
                    AudioRecord(
                        config.audioSource,
                        config.sampleRate,
                        config.channelConfig,
                        config.audioFormat,
                        bufferSize
                    ).also {
                        if (it.state != AudioRecord.STATE_INITIALIZED) {
                            throw IllegalStateException("AudioRecord failed to initialize. Check permissions or hardware issues.")
                        }
                    }

                fileOutputStream = FileOutputStream(outputFile, true)
                val audioBuffer = ByteArray(bufferSize)

                audioRecord?.startRecording()
                Timber.tag(LOG_TAG)
                    .d("Audio recording started, writing to: ${outputFile.absolutePath}")

                // Signal that setup is complete and recording has begun.
                recordingStartedSignal.complete(Unit)

                while (scope.isActive && isRecording.get()) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                    if (readSize > 0) {
                        val validData = audioBuffer.copyOf(readSize)
                        _audioStream.emit(validData)
                        fileOutputStream.write(validData)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during audio recording loop.")
                isRecording.set(false)
                // Ensure the caller doesn't hang if setup fails.
                if (!recordingStartedSignal.isCompleted) {
                    recordingStartedSignal.completeExceptionally(e)
                }
            }
            finally {
                Timber.d("Audio recording loop finished.")
                // The 'finally' block is the single source of truth for cleanup.
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null

                // Close the stream before updating the header.
                fileOutputStream?.close()

                // Update the header here, after everything is written and closed.
                recordingFile?.let {
                    WavFileUtil.updateWavHeader(it)
                    Timber.d("WAV header updated for file: ${it.absolutePath}")
                }
            }
        }
        // This await() waits for the signal from the background job.
        recordingStartedSignal.await()
        return outputFile to recordingJob
    }

    /**
     * Signals the recording loop to stop. The loop will then enter its `finally`
     * block to handle all cleanup and file finalization.
     */
    override fun stopRecording() {
        if (isRecording.compareAndSet(true, false)) {
            Timber.tag(LOG_TAG).d("Stopping recording...")
        }
    }


    companion object {
        private const val LOG_TAG = "AudioCaptureProvider"

        internal fun create(
            context: Context,
            coroutineScope: CoroutineScope,
            dispatcherProvider: DispatcherProvider
        ): AudioCaptureProvider {
            return AndroidAudioCaptureProvider(
                context = context,
                scope = coroutineScope,
                dispatcherProvider = dispatcherProvider,
            )
        }
    }
}