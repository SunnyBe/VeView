# Consumer ProGuard rules for the VeView SDK
# These rules are bundled with the AAR and applied to any app that uses this library.

# --- Public API Surface ---
# Keep the main entry point and its public nested classes (e.g., Builder).
-keep public class com.veview.veviewsdk.presentation.VeViewSDK { *; }
-keep public class com.veview.veviewsdk.presentation.VeViewSDK$* { *; }

# Keep the public interface for the voice reviewer.
-keep public interface com.veview.veviewsdk.domain.reviewer.VoiceReviewer { *; }

# --- State and Data Models ---
# Keep all public data model classes. These are part of the public API.
-keep public class com.veview.veviewsdk.domain.model.** { *; }

# Keep the entire VoiceReviewState sealed class hierarchy. This is crucial for consumers
# to be able to handle all possible states (Idle, Recording, Success, Error, etc.).
-keep class com.veview.veviewsdk.presentation.voicereview.VoiceReviewState { *; }
-keep class com.veview.veviewsdk.presentation.voicereview.VoiceReviewState$* { *; }

# --- Configuration Models ---
# The VoiceReviewConfig is parsed from JSON, which may use reflection.
# Keep this class and its members from being obfuscated or removed.
-keep class com.veview.veviewsdk.data.configs.VoiceReviewConfig { *; }
-keep class com.veview.veviewsdk.data.configs.VoiceReviewConfig$* { *; }

# --- Third-Party Library Dependencies ---
# The openai-kotlin SDK uses Ktor, which can be affected by shrinking.
# These are standard, safe rules to ensure Ktor's reflection-based features work correctly.
-keep class io.ktor.client.engine.okhttp.** { *; }
-dontwarn io.ktor.client.engine.okhttp.**
