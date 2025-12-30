# ProGuard rules for the veview-sdk module's test build (e.g., testReleaseUnitTest).
# These rules prevent R8 from removing classes and members needed by MockK via reflection.

# --- Keep Test Frameworks ---
# Keep testing libraries themselves from being stripped.
-keep class io.mockk.** { *; }
-keep class app.cash.turbine.** { *; }

# --- Keep Class Under Test ---
# The primary implementation class being tested.
-keep class com.veview.veviewsdk.domain.reviewer.VoiceReviewerImpl { *; }

# --- Keep Mocked Interfaces and Factories ---
# These interfaces are mocked in tests to isolate the class under test from its dependencies.
-keep interface com.veview.veviewsdk.domain.contracts.AnalysisEngine { *; }
-keep interface com.veview.veviewsdk.domain.contracts.AudioCaptureProvider { *; }
-keep interface com.veview.veviewsdk.domain.contracts.AudioCaptureProvider$Factory { *; }
-keep interface com.veview.veviewsdk.domain.contracts.ConfigProvider { *; }
-keep interface com.veview.veviewsdk.domain.contracts.DispatcherProvider { *; }

# --- Keep State and Data Models ---
# These are data/state classes used for setting up tests and verifying results.
# Using wildcards is robust for sealed class hierarchies like VoiceReviewState and AudioRecordState.
-keep class com.veview.veviewsdk.presentation.voicereview.** { *; }
-keep class com.veview.veviewsdk.domain.model.** { *; }
-keep class com.veview.veviewsdk.data.configs.** { *; }
