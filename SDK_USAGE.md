
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

// For the default analysis response
val voiceReviewer = veViewSDK.newAudioReviewer(
    context = context, // Activity or Application context
    config = config
)
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
                val reviewSummary = state.result.summary
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

## 🎨 Advanced Usage: Custom Analysis Response

The SDK allows you to define your own data structure for the analysis response, giving you full control over the data you receive.

### 1. Define Your Custom Data Class

Create a data class that represents the structure of the JSON response you want from the analysis. Use the `@FieldDescription` annotation to describe each field. This information is used to dynamically construct the prompt for the AI.

```kotlin
data class MainReviewResponse(
    @FieldDescription(
        text = "A concise, one-sentence summary of the entire review.",
        type = JsonType.STRING
    )
    val summary: String,

    @FieldDescription(
        text = "An integer rating from 1 to 5, where 1 is very negative and 5 is very positive, based on the user's sentiment.",
        type = JsonType.INTEGER
    )
    val rating: Int,

    @FieldDescription(
        text = "A list of specific positive points. If none are found, return an empty list [].",
        type = JsonType.LIST_OF_STRINGS
    )
    val pros: List<String>?,

    @FieldDescription(
        text = "A list of specific negative points. If none are found, return an empty list [].",
        type = JsonType.LIST_OF_STRINGS
    )
    val cons: List<String>?,

    @FieldDescription(
        text = "A short, polite, and professional reply suggestion. If no reply is appropriate, this field can be omitted.",
        type = JsonType.STRING
    )
    val suggestedReply: String?,

    @FieldDescription(
        text = "A list of keywords that categorize the review (e.g., 'service', 'price'). If no specific keywords can be extracted, return an empty list [].",
        type = JsonType.LIST_OF_STRINGS
    )
    val keywords: List<String>?,

    @FieldDescription(
        text = "Extract one or two exact, impactful quotes from the review. If no standout quotes are found, this field can be omitted.",
        type = JsonType.LIST_OF_STRINGS
    )
    val quotedHighlights: List<String>?
)
```

### 2. Use the Custom Audio Reviewer

Use the `newCustomAudioReviewer` method to create a `VoiceReviewer` instance that is typed to your custom data class.

```kotlin
val voiceReviewer = veViewSDK.newCustomAudioReviewer(
    context = application,
    responseType = MainReviewResponse::class.java,
    config = config
)
```

### 3. Observe the Custom State

When you collect the state from the custom reviewer, the `Success` state will contain your custom object.

```kotlin
lifecycleScope.launch {
    viewModel.voiceReviewState.collect { state ->
        when (state) {
            is VoiceReviewState.Success -> {
                val customResult = state.result // This is an instance of MainReviewResponse
                val rating = customResult.rating
                val summary = customResult.summary
                // ... and so on
            }
            // ... handle other states
        }
    }
}
```
