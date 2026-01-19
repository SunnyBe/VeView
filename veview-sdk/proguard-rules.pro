# ProGuard rules for the veview-sdk module's test build (e.g., testReleaseUnitTest).
# These rules prevent R8 from removing classes and members needed by MockK via reflection.

# --- Keep Test Frameworks ---
# Keep testing libraries themselves from being stripped.
-keep class io.mockk.** { *; }
-keep class app.cash.turbine.** { *; }

# --- Keep Class Under Test ---
# The primary implementation class being tested.
-keep class com.veview.veviewsdk.data.voicereview.VoiceReviewerImpl { *; }

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

-keep class com.veview.veviewsdk.presentation.VeViewSDK$Companion { *; }
-keep class com.veview.veviewsdk.presentation.VeViewSDK { *; }
-keep class com.veview.veviewsdk.domain.reviewer.VoiceReviewer { *; }

# --- Keep Moshi Reflective Adapter ---
# Moshi's reflective adapter needs to see Kotlin's metadata to function.
# This prevents R8 from stripping the metadata that moshi-kotlin-reflect relies on.
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }


-keep public class com.veview.veviewsdk.annotations.** { *; }
