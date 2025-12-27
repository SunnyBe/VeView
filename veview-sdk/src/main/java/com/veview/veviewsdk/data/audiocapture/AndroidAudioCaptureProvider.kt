package com.veview.veviewsdk.data.audiocapture

import android.Manifest
import android.content.Context
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.data.coroutine.DefaultDispatcherProvider
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
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
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

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
    private val _audioStream = MutableSharedFlow<ByteArray>()
    override val audioStream: Flow<ByteArray> = _audioStream.asSharedFlow()

    private var audioRecord: AudioRecord? = null
    private var isRecording = AtomicBoolean(false)
    private var recordingFile: File? = null // Keep track of the file

    private var recordingJob: Job? = null

    @Suppress("LongMethod")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    // 2. THE FIX: The function now only returns the File, as the Job is managed internally.
    override suspend fun startRecording(
        fileName: String,
        config: VoiceReviewConfig
    ): File {
        Timber.tag(LOG_TAG).d("Current Config: $config")
        check(isRecording.compareAndSet(false, true)) { "There is an ongoing recording." }

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

        // Assign the launched job to our internal property.
        recordingJob = scope.launch(dispatcherProvider.io) {
            var fileOutputStream: FileOutputStream? = null
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    config.sampleRate,
                    config.channelConfig,
                    config.audioFormat
                )
                val bufferSize = minBufferSize * 2
                check(bufferSize > 0) { "Invalid buffer size calculated: $bufferSize" }

                audioRecord = AudioRecord(
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

                fileOutputStream = FileOutputStream(outputFile, true)
                val audioBuffer = ByteArray(bufferSize)

                audioRecord?.startRecording()
                Timber.tag(LOG_TAG)
                    .d("Audio recording started, writing to: ${outputFile.absolutePath}")
                recordingStartedSignal.complete(Unit)

                while (scope.isActive && isRecording.get()) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                    if (readSize > 0) {
                        val validData = audioBuffer.copyOf(readSize)
                        _audioStream.emit(validData)
                        fileOutputStream.write(validData)
                    }
                }
            } catch (e: CancellationException) {
                // This is the correct way to handle cancellation. Let it propagate.
                Timber.tag(LOG_TAG).d("Recording job was cancelled.")
                throw e
            } catch (e: IOException) {
                // 2. THE FIX: Catch specific, expected exceptions.
                Timber.e(e, "An IO error occurred during audio recording.")
                isRecording.set(false)
                if (!recordingStartedSignal.isCompleted) {
                    recordingStartedSignal.completeExceptionally(e)
                }
                throw e // Propagate the error to the caller
            } catch (e: IllegalStateException) {
                // Catch errors from our `check` calls.
                Timber.e(e, "An IllegalStateException occurred during audio setup.")
                isRecording.set(false)
                if (!recordingStartedSignal.isCompleted) {
                    recordingStartedSignal.completeExceptionally(e)
                }
                throw e // Propagate the error to the caller
            } finally {
                Timber.d("Audio recording loop finished.")
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                fileOutputStream?.close()
                recordingFile?.let {
                    WavFileUtil.updateWavHeader(it)
                    Timber.d("WAV header updated for file: ${it.absolutePath}")
                }
            }
        }

        recordingStartedSignal.await()
        return outputFile
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

    override fun cancel() {
        Timber.tag(LOG_TAG).d("Cancelling audio capture job.")
        recordingJob?.cancel()
        isRecording.set(false)
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
}
