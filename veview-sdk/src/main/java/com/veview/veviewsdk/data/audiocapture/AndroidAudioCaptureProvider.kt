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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

    @Suppress("SwallowedException")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording(
        fileName: String,
        config: VoiceReviewConfig,
        duration: Duration
    ): Flow<AudioRecordState> = callbackFlow {
        Timber.tag(LOG_TAG).d("Current Config: $config")

        if (!isRecording.compareAndSet(false, true)) {
            close(IllegalStateException("There is an ongoing recording."))
            return@callbackFlow
        }

        trySend(AudioRecordState.Starting) // Emit starting state

        val outputFile = generateOutputFile(config, fileName)

        // Minimum buffer size to avoid buffer underruns on this device
        val minBufferSize = AudioRecord.getMinBufferSize(
            config.sampleRate,
            config.channelConfig,
            config.audioFormat
        )
        val bufferSize = minBufferSize * 2 // Use double the minimum buffer size for safety

        check(bufferSize > 0) { "Invalid buffer size calculated: $bufferSize" }

        val record = createAudioRecordInstance(config, bufferSize)
        audioRecord = record

        val fileOutputStream = FileOutputStream(outputFile, true)
        val audioBuffer = ByteArray(bufferSize)
        val startTime = System.now() // Note start time

        record.startRecording()
        trySend(AudioRecordState.Started(outputFile, startTime))
        Timber.tag(LOG_TAG).d("Started audio recording at $startTime.")

        val job = launch(dispatcherProvider.io) {
            try {
                while (isActive && isRecording.get() && (System.now() - startTime) < duration) {
                    val readSize = record.read(audioBuffer, 0, audioBuffer.size)

                    if (readSize < 0) {
                        throw IllegalStateException("AudioRecord error code: $readSize")
                    }
                    if (readSize > 0) {
                        val validData = audioBuffer.copyOf(readSize)
                        fileOutputStream.write(validData)

                        trySend(
                            AudioRecordState.DataChunkReady(
                                outputFile,
                                validData,
                                System.now() - startTime
                            )
                        )
                    }
                }
            } catch (cause: IOException) {
                close(cause)
            } catch (cause: IllegalStateException) {
                close(cause)
            } finally {
                isRecording.set(false)

                try { record.stop() } catch (e: IllegalStateException) { /* logged ignore */ }

                try {
                    fileOutputStream.close()
                    trySend(AudioRecordState.Stopped(System.now()))
                    WavFileUtil.updateWavHeader(outputFile)
                    trySend(AudioRecordState.Done(outputFile))
                } catch (e: IOException) {
                    Timber.tag(LOG_TAG).e(e, "Failed to finalize audio file.")
                }
                if (!channel.isClosedForSend) channel.close()
            }
        }

        awaitClose {
            Timber.tag(LOG_TAG).d("callbackFlow closing, cleaning up.")
            isRecording.set(false)
            job.cancel()
            record.release()
            audioRecord = null
        }
    }.catch { cause ->
        Timber.e(cause, "Error in audio recording flow.")
        isRecording.set(false)
        if (cause is CancellationException) {
            throw cause // Stop subsequent processing on cancellation
        } else {
            val exception = AudioRecordingException(cause.message, cause)
            emit(AudioRecordState.Error(exception))
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
