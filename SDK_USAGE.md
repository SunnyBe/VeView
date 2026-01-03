
# Using the VeView SDK

This guide provides detailed instructions on how to integrate and use the VeView SDK in your Android application.

## 🚀 Getting Started

### 1. Add the Dependency

Add the following to your `build.gradle.kts` or `build.gradle` file.

**Example for Jitpack:**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.SunnyBe:VeView:<latest-version>")
}
```

### 2. Add Permissions

Declare the required permissions in your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 3. Initialize the SDK

Initialize the `VoiceReviewer` in your `Application`, `Activity`, or `Fragment`. It's recommended to do this in a `ViewModel` to persist it across configuration changes.

```kotlin

val veViewSDK = VeViewSDK.Builder(
    apiKey = "OPENAI_API_KEY_HERE",
    isDebug = true // Mainly for logging purposes
).build()

val config = VoiceReviewConfig.Builder()
    .setRecordDuration(1.minutes)
    .build()

val voiceReviewer = veViewSDK.newAudioReviewer(
    context = context, // Activity or Application context
    config = config
)

class MyReviewViewModel(voiceReviewer: VoiceReviewer) : ViewModel {
    
    val voiceReviewState: StateFlow<VoiceReviewState> = voiceReviewer.state

    fun startVoiceReview(reviewId: String) {
        voiceReviewer.start(ReviewContext(reviewId = reviewId))
    }
}
```

## 🎤 Usage

### Start a Voice Review

To start a voice review, simply call the `start` method.

```kotlin
viewModel.startVoiceReview("review-id-12345")
```

### Observe the State

Collect the `StateFlow<VoiceReviewState>` to react to changes in the voice review process and update your UI accordingly.

```kotlin
lifecycleScope.launch {
    viewModel.voiceReviewState.collect { state ->
        when (state) {
            is VoiceReviewState.Idle -> { /* SDK is ready to start a new recording */ }
            is Voice.Recording -> {
                val progress = state.progress // 0.0 to 1.0
                /* Update UI to show recording is in progress */
            }
            is VoiceReviewState.Processing -> { /* The recording is being processed and transcribed */ }
            is VoiceReviewState.Success -> {
                val reviewSummary = state.reviewSummary
                /* Voice review completed, you have the text! */
            }
            is VoiceReviewState.Error -> {
                val errorMessage = state.cause
                /* An error occurred, handle it appropriately */
            }
        }
    }
}
```
