package com.veview.veviewsdk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.ServiceTestRule
import com.google.common.truth.Truth.assertThat
import com.veview.veviewsdk.data.audiocapture.AndroidAudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.utils.EmptyTestService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeoutException

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
    fun startRecording_and_stopRecording_creates_a_valid_nonEmpty_wavFile() = runBlocking {
        // Arrange
        val fileName = "instrumentation_test_${System.currentTimeMillis()}.wav"
        val expectedRecordDuration = 2000L // Record for 2 seconds

        // Act
        val config = VoiceReviewConfig.Builder()
            .setStorageDirectory(applicationContext.cacheDir)
            .setChannelConfig(AudioFormat.CHANNEL_IN_MONO) // Use the Android constant
            .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT) // Use the Android constant
            .setSampleRate(16000)
            .build()

        val audioFile = audioProvider.startRecording(fileName, config)
        createdFile = audioFile // Save reference for cleanup
        delay(expectedRecordDuration)
        audioProvider.stopRecording()
        delay(500)
        // Assert
        assertThat(audioFile.exists()).isTrue()
        assertThat(audioFile.length()).isGreaterThan(44L) // Must be larger than the WAV header

        // 6. Verify the file path is correct
        val expectedParentDir = applicationContext.cacheDir
        assertThat(audioFile.parentFile).isEqualTo(expectedParentDir)
        assertThat(audioFile.name).isEqualTo(fileName)

        // 7. Optional: A more advanced check could be to read the WAV header
        // and verify the file size fields were updated correctly.
        val headerBytes = audioFile.readBytes().take(44)
        // For example, check that the RIFF chunk size (bytes 4-7) is not zero.
        val chunkSize =
            headerBytes.slice(4..7).reversed().joinToString("") { "%02x".format(it) }.toLong(16)
        assertThat(chunkSize).isEqualTo(audioFile.length() - 8)
    }
}
