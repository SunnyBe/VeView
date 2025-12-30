# VeView SDK for Android

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Latest Version](https://img.shields.io/badge/version-v0.0.1-blue.svg)](<fill content manually: link to releases>)

The VeView SDK for Android provides a simple and powerful way to integrate voice feedback and review capabilities into your Android application. It handles audio recording, processing, and AI-powered transcription, allowing you to focus on building your app's core features.

## ✨ Features

*   **Effortless Integration**: Add voice review functionality to your app in minutes.
*   **AI-Powered Transcription**: Converts spoken reviews into text using state-of-the-art AI.
*   **Modern Architecture**: Built with Kotlin, Coroutines, and Flow for a reactive, non-blocking, and thread-safe solution.
*   **Robust State Management**: Observe the entire voice review lifecycle through a `StateFlow`, making UI updates a breeze.
*   **Lifecycle Aware**: Seamlessly integrates with Android's lifecycle components (`ViewModel`, `Activity`, `Fragment`) to handle configuration changes gracefully.
*   **Highly Configurable**: Customize recording duration, storage options, and more.
*   **Dynamic Configuration**: Alter SDK behavior on the fly using local or remote configuration providers.
*   **Testable**: Designed for testability, with decoupled business logic and hardware interactions.

## 🚀 Getting Started

### 1. Add the Dependency

Add the following to your `build.gradle.kts` or `build.gradle` file.

`<fill content manually: Add your hosting instructions here, e.g., Maven Central, JitPack, etc.>`

**Example for Maven Central:**

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.veview:veview-sdk:0.0.1") // Replace with the latest version
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
class MyReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceReviewer: VoiceReviewer = VoiceReviewer.create(
        context = application,
        apiKey = "<fill content manually: YOUR_VEVIEW_API_KEY>",
        okHttpClient = OkHttpClient(), // Optional: Provide your own OkHttpClient instance
        configProvider = LocalConfigProviderImpl(application) // Optional: Provide a custom config provider
    )

    val voiceReviewState: StateFlow<VoiceReviewState> = voiceReviewer.state

    fun startVoiceReview(activity: Activity, reviewId: String) {
        voiceReviewer.start(activity, ReviewContext(reviewId = reviewId))
    }
}
```

## 🎤 Usage

### Start a Voice Review

To start a voice review, simply call the `start` method.

```kotlin
viewModel.startVoiceReview(this, "review-id-12345")
```

### Observe the State

Collect the `StateFlow<VoiceReviewState>` to react to changes in the voice review process and update your UI accordingly.

```kotlin
lifecycleScope.launch {
    viewModel.voiceReviewState.collect { state ->
        when (state) {
            is VoiceReviewState.Idle -> { /* SDK is ready to start a new recording */ }
            is VoiceReviewState.Recording -> {
                val progress = state.progress // 0.0 to 1.0
                /* Update UI to show recording is in progress */
            }
            is VoiceReviewState.Processing -> { /* The recording is being processed and transcribed */ }
            is VoiceReviewState.Success -> {
                val transcription = state.transcription
                /* Voice review completed, you have the text! */
            }
            is VoiceReviewState.Error -> {
                val errorMessage = state.message
                /* An error occurred, handle it appropriately */
            }
        }
    }
}
```

## ⚙️ Configuration

You can easily override the default SDK settings by creating a `veview_config.json` file in your app's `res/raw` directory.

**Example `res/raw/veview_config.json`:**

```json
{
  "maxRecordingDurationMillis": 60000,
  "removeFileAfterAnalysis": true
}
```

## 🤝 Contributor Setup

To ensure a consistent development environment, we use Gradle's dependency locking. After cloning the repository, run the following script to generate the dependency lock files:

```bash
./generate_dependency_locks.sh generate
```

This will create a `gradle.lockfile` in each module's directory. These files should be committed to version control.

## 📦 Third-Party Dependencies

For a full list of third-party libraries used in this SDK, please see the [DEPENDENCIES.md](veview-sdk/DEPENDENCIES.md) file.

## 📄 License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.
