package com.veview.veviewsdk

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import com.veview.veviewsdk.data.voicereview.VoiceReviewState
import com.veview.veviewsdk.data.voicereview.VoiceReviewerImpl
import com.veview.veviewsdk.domain.contracts.AnalysisEngine
import com.veview.veviewsdk.domain.contracts.AudioCaptureProvider
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import com.veview.veviewsdk.domain.contracts.DispatcherProvider
import com.veview.veviewsdk.domain.model.AudioRecordState
import com.veview.veviewsdk.domain.model.ReviewContext
import com.veview.veviewsdk.domain.model.VoiceReview
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@ExperimentalCoroutinesApi
class VoiceReviewerImplTest {

    // --- Mocks and Test Doubles ---
    private val mockAudioProviderFactory: AudioCaptureProvider.Factory = mockk()
    private val mockConfigProvider: ConfigProvider = mockk()
    private val mockAnalysisEngine: AnalysisEngine<VoiceReview> = mockk()
    private val mockAudioProvider: AudioCaptureProvider = mockk(relaxed = true)

    private val fakeAudioFile = File("fake/path/review_12345.wav")
    private val fakeReviewSubject = ReviewContext.ReviewSubject("Harry Kitchen")

    // --- Coroutine Test Setup ---
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    // --- Class Under Test ---
    private lateinit var voiceReviewer: VoiceReviewerImpl<VoiceReview>

    @Before
    fun setUp() {
        every { mockAudioProviderFactory.create(any()) } returns mockAudioProvider

        val defaultConfig = VoiceReviewConfig.Builder().setRecordDuration(1.seconds).build()
        every { mockConfigProvider.voiceReviewConfigFlow } returns flowOf(defaultConfig)
        every { mockAudioProvider.startRecording(any(), any(), any()) } returns flowOf(
            AudioRecordState.Starting,
            AudioRecordState.Started(fakeAudioFile, Instant.parse("2024-01-01T00:00:00Z")),
            AudioRecordState.DataChunkReady(fakeAudioFile, byteArrayOf(1, 2, 3), 500.milliseconds),
            AudioRecordState.Done(fakeAudioFile)
        )

        // Create the class under test, injecting all mocks.
        // This assumes the VoiceReviewerImpl constructor has been updated to accept `audioProvider`.
        voiceReviewer = VoiceReviewerImpl(
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
            val reviewContext = ReviewContext("product-123", fakeReviewSubject)
            val fakeReview =
                VoiceReview("transcript", "summary", 5, emptyList(), emptyList(), fakeAudioFile)
            coEvery { mockAnalysisEngine.analyze(any(), any()) } returns fakeReview

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

                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Processing(fakeAudioFile))
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Success(fakeReview))

                // Verify mocks were called in the correct sequence
                coVerify { mockAudioProvider.startRecording(any(), any(), any()) }
                coVerify { mockAnalysisEngine.analyze(fakeAudioFile, any()) }
            }
        }

    @Test
    fun `analysis engine throwing exception transitions to Error state`() = testScope.runTest {
        val exception = RuntimeException("Network failed")
        coEvery { mockAnalysisEngine.analyze(any(), any()) } throws exception

        voiceReviewer.state.test {
            awaitItem() // Idle
            voiceReviewer.start(ReviewContext("product-123", fakeReviewSubject))
            awaitItem() // Initializing
            runCurrent()
            awaitItem() // Recording

            testDispatcher.scheduler.advanceTimeBy(1000)

            awaitItem() // Processing

            // The final state should be Error
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(VoiceReviewState.Error::class.java)
            assertThat((errorState as VoiceReviewState.Error).throwable?.message).isEqualTo(
                exception.message
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel() during recording transitions state to Cancelled deterministically`() =
        testScope.runTest {
            every { mockAudioProvider.startRecording(any(), any(), any()) } returns flowOf(
                AudioRecordState.Starting,
                AudioRecordState.Started(fakeAudioFile, Instant.parse("2024-01-01T00:00:00Z")),
                AudioRecordState.DataChunkReady(fakeAudioFile, byteArrayOf(1, 2, 3), 3.seconds)
                // No done state as recording is still ongoing
            )

            voiceReviewer.state.test {
                awaitItem() // Idle

                voiceReviewer.start(ReviewContext("p1", fakeReviewSubject))

                awaitItem() // Initializing
                runCurrent() // Launch session job is pending, let's proceed to process it
                assertThat(awaitItem()).isInstanceOf(VoiceReviewState.Recording::class.java)

                // Act: Cancel while recording is ongoing
                voiceReviewer.cancel()

                // Assert: The very next state must be Cancelled
                assertThat(awaitItem()).isEqualTo(VoiceReviewState.Cancelled)

                // Ensure no other events can occur
                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `calling start while a session is active emits an error`() = testScope.runTest {
        every { mockAudioProvider.startRecording(any(), any(), any()) } returns flow {
            emit(AudioRecordState.Starting)
            emit(AudioRecordState.Started(fakeAudioFile, Instant.parse("2024-01-01T00:00:00Z")))
            emit(AudioRecordState.DataChunkReady(fakeAudioFile, byteArrayOf(1, 2, 3), 5.seconds))
            // No Done state to simulate ongoing recording
            delay(1.seconds)
//            emit(AudioRecordState.Done(fakeAudioFile))
        }

        // Act
        voiceReviewer.state.test {
            assertThat(awaitItem()).isEqualTo(VoiceReviewState.Idle)

            // First start call
            voiceReviewer.start(ReviewContext("first", fakeReviewSubject))

            awaitItem() // Initializing

            runCurrent() // Ren tasks scheduled by launch, allowing the collect to start
            awaitItem() // Recording

            testDispatcher.scheduler.advanceUntilIdle() // advance to end of collection

            // Second start call while the first is running
            voiceReviewer.start(ReviewContext("second", fakeReviewSubject))

            // Assert that an error state was emitted
            val errorItem = awaitItem()
            assertThat(errorItem is VoiceReviewState.Error)
            assertThat((errorItem as VoiceReviewState.Error).message).contains("active recording session")

            // Cancel to clean up the test
            cancelAndConsumeRemainingEvents()
        }
    }
}
