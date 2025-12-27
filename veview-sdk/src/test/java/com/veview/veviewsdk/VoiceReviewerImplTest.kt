package com.veview.veviewsdk

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.model.VoiceReview
import com.veview.veviewsdk.domain.model.VoiceReviewError
import com.veview.veviewsdk.domain.reviewer.VoiceReviewerImpl
import com.veview.veviewsdk.presentation.voicereview.VoiceReviewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class VoiceReviewerImplTest {

    // --- Mocks and Test Doubles ---
    private val mockAudioProviderFactory: AudioCaptureProvider.Factory = mockk()
    private val mockConfigProvider: ConfigProvider = mockk()
    private val mockAnalysisEngine: AnalysisEngine = mockk()
    private val mockAudioProvider: AudioCaptureProvider = mockk(relaxed = true)

    private val fakeAudioFile = File("fake/path/review_12345.wav")

    // --- Coroutine Test Setup ---
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    // --- Class Under Test ---
    private lateinit var voiceReviewer: VoiceReviewerImpl

    @Before
    fun setUp() {
        every { mockAudioProviderFactory.create(any()) } returns mockAudioProvider

        val defaultConfig = VoiceReviewConfig.Builder().setRecordDuration(1.seconds).build()
        every { mockConfigProvider.voiceReviewConfigFlow } returns flowOf(defaultConfig)
        coEvery { mockAudioProvider.startRecording(any(), any()) } returns fakeAudioFile
        every { mockAudioProvider.audioStream } returns MutableSharedFlow() // A hot flow that does nothing

        // Create the class under test, injecting all mocks.
        // This assumes the VoiceReviewerImpl constructor has been updated to accept `audioProvider`.
        voiceReviewer = VoiceReviewerImpl(
            apiKey = "test-api-key",
            configProvider = mockConfigProvider,
            analysisEngine = mockAnalysisEngine,
            audioProviderFactory = mockAudioProviderFactory,
            coroutineScope = testScope.backgroundScope,
            coroutineDispatcher = testDispatcherProvider
        )
    }

    @Test
    fun `initial state is Idle`() = testScope.runTest {
        assertThat(voiceReviewer.state.value).isEqualTo(VoiceReviewState.Idle)
    }

    @Test
    fun `start() transitions through Initializing, Recording, Processing, and Success`() =
        testScope.runTest {
            val reviewContext = ReviewContext("product-123", "Restaurant", true)
            val fakeReview =
                VoiceReview("transcript", "summary", 5, emptyList(), emptyList(), fakeAudioFile)
            coEvery { mockAnalysisEngine.analyze(any()) } returns fakeReview

            // Act & Assert
            voiceReviewer.state.test {
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Idle)
                voiceReviewer.start(reviewContext)
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Initializing(reviewContext))

                runCurrent()

                val recordingState = awaitItem()
                assertThat(recordingState).isInstanceOf(VoiceReviewState.Recording::class.java)
                assertThat((recordingState as VoiceReviewState.Recording).file).isEqualTo(
                    fakeAudioFile
                )

                testDispatcher.scheduler.advanceTimeBy(1001)

                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Processing(fakeAudioFile))
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Success(fakeReview))

                // Verify mocks were called in the correct sequence
                coVerify { mockAudioProvider.startRecording(any(), any()) }
                verify { mockAudioProvider.stopRecording() }
                coVerify { mockAnalysisEngine.analyze(fakeAudioFile) }
            }
        }

    @Test
    fun `start with blank API key throws AuthenticatorException and transitions to Error`() =
        testScope.runTest {
            // Arrange - Re-create the SUT for this specific edge case.
            voiceReviewer = VoiceReviewerImpl(
                apiKey = " ", // Blank API key
                configProvider = mockConfigProvider,
                analysisEngine = mockAnalysisEngine,
                audioProviderFactory = mockAudioProviderFactory, // Still need to inject the mock
                coroutineScope = testScope,
                coroutineDispatcher = testDispatcherProvider
            )
            val reviewContext = ReviewContext("product-123", "Restaurant", true)

            // Act & Assert
            voiceReviewer.state.test {
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Idle)

                voiceReviewer.start(reviewContext)

                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Initializing(reviewContext))

                val errorState = awaitItem()
                assertThat(errorState).isInstanceOf(VoiceReviewState.Error::class.java)
                assertThat((errorState as VoiceReviewState.Error).errorType).isEqualTo(
                    VoiceReviewError.INVALID_API_KEY
                )
            }
        }

    @Test
    fun `analysis engine throwing exception transitions to Error state`() = testScope.runTest {
        val exception = RuntimeException("Network failed")
        coEvery { mockAnalysisEngine.analyze(any()) } throws exception

        voiceReviewer.state.test {
            awaitItem() // Idle
            voiceReviewer.start(ReviewContext("product-123", "Restaurant", true))
            awaitItem() // Initializing
            runCurrent()
            awaitItem() // Recording

            testDispatcher.scheduler.advanceTimeBy(1000)

            awaitItem() // Processing

            // The final state should be Error
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(VoiceReviewState.Error::class.java)
            assertThat((errorState as VoiceReviewState.Error).throwable).isEqualTo(exception)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel() during recording transitions state to Cancelled deterministically`() =
        testScope.runTest {
            val longDurationConfig =
                VoiceReviewConfig.Builder().setRecordDuration(30.seconds).build()
            every { mockConfigProvider.voiceReviewConfigFlow } returns flowOf(longDurationConfig)

            voiceReviewer.state.test {
                awaitItem() // Idle

                voiceReviewer.start(ReviewContext("p1", "f1", false))

                awaitItem() // Initializing
                runCurrent()
                assertThat(awaitItem()).isInstanceOf(VoiceReviewState.Recording::class.java)

                // Act: Cancel while recording is "paused"
                voiceReviewer.cancel()

                // Assert: The very next state must be Cancelled
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Cancelled)

                // Ensure no other events can occur
                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `calling start while a session is active does nothing`() = testScope.runTest {
        // Act
        voiceReviewer.state.test {
            assertThat(awaitItem()).isEqualTo(VoiceReviewState.Idle)

            // First start call
            voiceReviewer.start(ReviewContext("first", "General", false))
            awaitItem() // Initializing
            awaitItem() // Recording

            // Second start call while the first is running
            voiceReviewer.start(ReviewContext("second", "General", false))

            // Assert that no new state is emitted because the first session is active
            expectNoEvents()

            // Cancel to clean up the test
            cancelAndConsumeRemainingEvents()
        }
    }
}
