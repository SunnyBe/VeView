# VeView SDK for Android

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The VeView SDK for Android is a modern, coroutine-based library designed to simplify the process of capturing, processing, and analyzing voice reviews within any Android application.

## ✨ Features

*   **State-of-the-Art Architecture**: Built with Kotlin Coroutines, Flow, and a clean, layered architecture.
*   **Highly Configurable**: Easily configure recording duration, audio quality, and storage options.
*   **Dynamic Configuration**: Supports dynamic configuration providers (e.g., from a local QA settings screen or remote config) to change SDK behavior on the fly.
*   **Robust State Management**: Exposes a simple `StateFlow<VoiceReviewState>` to observe the entire lifecycle of a voice review, from `Idle` to `Success` or `Error`.
*   **Lifecycle Aware**: Designed to integrate seamlessly with Android's modern lifecycle components like `ViewModel` to survive configuration changes without interrupting recordings.
*   **Testable**: Core business logic is fully unit-testable, and hardware interactions are validated with instrumentation tests.

## 🚀 Getting Started

### 1. Add the Dependency

To get started, add the VeView SDK as a dependency in your app's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation(project(":veview-sdk"))
}
```

### 2. Add Permissions

The VeView SDK requires the following permissions to be declared in your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 3. Initialize the SDK

Initialize the `VoiceReviewer` in your `Activity` or `Fragment`:

```kotlin
class MainActivity : ComponentActivity() {

    private lateinit var voiceReviewer: VoiceReviewer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceReviewer = VoiceReviewer.create(
            context = this,
            apiKey = "YOUR_API_KEY", // TODO: Replace with your API key
            okHttpClient = OkHttpClient(),
            configProvider = LocalConfigProviderImpl(this)
        )
    }
}
```

##  Usage

### Start a Voice Review

To start a voice review, call the `start` method with a `ReviewContext`:

```kotlin
voiceReviewer.start(this, ReviewContext(reviewId = "12345")) // TODO: Replace with a real review ID
```

### Observe the State

The `VoiceReviewer` exposes a `StateFlow<VoiceReviewState>` that you can collect to observe the state of the voice review process. This is useful for updating your UI based on the current state.

```kotlin
lifecycleScope.launch {
    voiceReviewer.state.collect { state ->
        when (state) {
            is VoiceReviewState.Idle -> { /* Ready to start */ }
            is VoiceReviewState.Recording -> { /* Recording in progress */ }
            is VoiceReviewState.Processing -> { /* Processing the recording */ }
            is VoiceReviewState.Success -> { /* Voice review completed successfully */ }
            is VoiceReviewState.Error -> { /* An error occurred */ }
        }
    }
}
```

### Using a ViewModel

It is highly recommended to use the `VoiceReviewer` inside an Android `ViewModel` to ensure that any ongoing recording or analysis survives screen rotations and other configuration changes.

```kotlin
class MyReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceReviewer: VoiceReviewer = VoiceReviewer.create(
        context = application,
        apiKey = "YOUR_API_KEY", // TODO: Replace with your API key
        okHttpClient = OkHttpClient(),
        configProvider = LocalConfigProviderImpl(application)
    )

    val voiceReviewState: StateFlow<VoiceReviewState> = voiceReviewer.state

    fun startVoiceReview(activity: Activity, reviewId: String) {
        voiceReviewer.start(activity, ReviewContext(reviewId = reviewId))
    }
}
```

## ⚙️ Configuration

The `LocalConfigProviderImpl` allows you to configure the SDK's behavior. You can create a `veview_config.json` file in your app's `res/raw` directory to override the default settings:

```json
{
  "maxRecordingDurationMillis": 30000,
  "removeFileAfterAnalysis": true
}
```

## 🤝 Contributor Setup

To ensure a consistent development environment, we use Gradle's dependency locking. After cloning the repository, run the following script to generate the dependency lock files:

```bash
./generate_dependency_locks.sh
```

This will create a `gradle.lockfile` in each module's directory. These files should be committed to version control.

## 📦 Third-Party Dependencies

For a full list of third-party libraries used in this SDK, please see the [DEPENDENCIES.md](veview-sdk/DEPENDENCIES.md) file.
