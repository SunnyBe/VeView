package com.veview.veviewsdk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.ServiceTestRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.veview.veviewsdk.data.audiocapture.AndroidAudioCaptureProvider
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.AudioRecordState
import com.veview.veviewsdk.utils.EmptyTestService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class AndroidAudioProviderTest {

    @get:Rule(order = 0)
    val serviceRule = ServiceTestRule()

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE
    )

    private lateinit var testScope: CoroutineScope
    private lateinit var dispatcherProvider: DispatcherProvider

    private lateinit var audioProvider: AudioCaptureProvider

    private var createdFile: File? = null
    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        dispatcherProvider = object : DispatcherProvider {
            override val main: CoroutineDispatcher = Dispatchers.Main
            override val io: CoroutineDispatcher = Dispatchers.IO
            override val default: CoroutineDispatcher = Dispatchers.Default
        }
        testScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

        try {
            val serviceIntent = Intent(applicationContext, EmptyTestService::class.java)
            applicationContext.startForegroundService(serviceIntent)
            val binder = serviceRule.bindService(serviceIntent)
            val service = (binder as EmptyTestService.LocalBinder).getService()

            audioProvider = AndroidAudioCaptureProvider
                .apply { initialize(service) }
                .create(testScope)
        } catch (e: TimeoutException) {
            throw IllegalStateException("Failed to bind to the EmptyTestService. The test cannot run.", e)
        }
    }

    @After
    fun tearDown() {
        if (::testScope.isInitialized) testScope.cancel()
        createdFile?.delete()
    }

    @Test
    fun startRecording_emitsStatesAndProducesValidWavFile() = runBlocking {
        // Arrange
        val fileName = "instrumentation_test_${System.currentTimeMillis()}.wav"
        val recordDuration = 2.seconds // Record for 2 real seconds

        val config = VoiceReviewConfig.Builder()
            .setStorageDirectory(applicationContext.cacheDir)
            .setChannelConfig(AudioFormat.CHANNEL_IN_MONO)
            .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(16000)
            .build()

        var finalAudioFile: File? = null

        // Act & Assert
        // Use Turbine's `test` extension to collect and assert flow emissions.
        audioProvider.startRecording(fileName, config, recordDuration).test {
            // The flow should immediately start emitting states
            assertThat(awaitItem()).isInstanceOf(AudioRecordState.Starting::class.java)
            assertThat(awaitItem()).isInstanceOf(AudioRecordState.Started::class.java)

            // We expect a series of data chunks
            var currentState = awaitItem()
            while (currentState is AudioRecordState.DataChunkReady) {
                currentState = awaitItem()
            }

            // The flow should eventually emit Stopped and then Done
            assertThat(currentState).isInstanceOf(AudioRecordState.Stopped::class.java)

            val doneState = awaitItem()
            assertThat(doneState).isInstanceOf(AudioRecordState.Done::class.java)

            // Capture the finalized file from the Done state
            finalAudioFile = (doneState as AudioRecordState.Done).audioFile
            createdFile = finalAudioFile

            // The flow should now be complete
            awaitComplete()
        }

        // Final assertions on the resulting file
        val audioFile = finalAudioFile!!
        assertThat(audioFile.exists()).isTrue()
        assertThat(audioFile.length()).isGreaterThan(44L) // Must be larger than the WAV header

        val headerBytes = audioFile.readBytes().take(44)
        val chunkSize =
            headerBytes.slice(4..7).reversed().joinToString("") { "%02x".format(it) }.toLong(16)
        assertThat(chunkSize).isEqualTo(audioFile.length() - 8)
    }

    @Test
    fun startRecording_withInvalidConfig_emitsErrorState() = runBlocking {
        // Arrange
        val fileName = "error_test.wav"
        val recordDuration = 5.seconds

        // Create an invalid configuration that AudioRecord will reject.
        // A sample rate of 0 is guaranteed to fail.
        val invalidConfig = VoiceReviewConfig.Builder()
            .setStorageDirectory(applicationContext.cacheDir)
            .setSampleRate(0) // Invalid sample rate
            .build()

        // Act & Assert
        audioProvider.startRecording(fileName, invalidConfig, recordDuration).test {
            // It should still try to start
            assertThat(awaitItem()).isInstanceOf(AudioRecordState.Starting::class.java)

            // It should then immediately emit an Error state because AudioRecord will fail to initialize
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(AudioRecordState.Error::class.java)

            // The flow should then complete because it has failed and cannot continue.
            awaitComplete()
        }
    }
}
