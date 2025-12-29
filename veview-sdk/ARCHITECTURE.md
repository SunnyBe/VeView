# Architecture Overview

This document provides a high-level overview of the VeView SDK's architecture. The SDK is designed to be modular, scalable, and easy to maintain.

## Core Principles

- **Separation of Concerns**: The codebase is divided into distinct layers (`core`, `data`, `domain`) to separate UI-facing logic, business logic, and data handling.
- **Reactive and Asynchronous**: The SDK uses Kotlin Coroutines and `StateFlow` to manage state and handle asynchronous operations in a non-blocking way.
- **Dependency Injection**: Dependencies are provided explicitly, making components testable and interchangeable. We use manual dependency injection to keep the SDK lightweight and avoid framework lock-in.
- **Immutability**: State objects are immutable to ensure thread safety and predictable state transitions.

## Architecture Layers

### 1. Core Layer (`com.veview.veviewsdk.core`)

This is the primary entry point and state management layer of the SDK.

- **`VoiceReviewer`**: The main public-facing class that clients interact with. It orchestrates the entire voice review process, from starting the recording to delivering the final transcription. It holds a `MutableStateFlow` to manage and expose the current `VoiceReviewState`.
- **`VoiceReviewState`**: A sealed class representing all possible states of the voice review lifecycle (e.g., `Idle`, `Recording`, `Processing`, `Success`, `Error`). This allows consumers of the SDK to reactively update their UI based on the SDK's internal state.

### 2. Domain Layer (`com.veview.veviewsdk.domain`)

This layer contains the core, framework-agnostic business logic of the SDK.

- **Use Cases/Interactors**: Classes that encapsulate specific business operations (e.g., `StartRecordingUseCase`, `TranscribeAudioUseCase`). They are orchestrated by the `VoiceReviewer`.
- **Entities**: Plain Kotlin data classes representing the core concepts of the SDK (e.g., `Transcription`, `ReviewConfig`).

### 3. Data Layer (`com.veview.veviewsdk.data`)

This layer is responsible for all data I/O, abstracting the data sources from the rest of the SDK.

- **`VoiceReviewRepository`**: A repository that provides a single source of truth for voice review data. It abstracts away the underlying data sources (network, local storage).
- **Data Sources**:
    - **`AudioCaptureSource`**: An interface for capturing audio. The concrete implementation (`WavAudioCaptureSource`) handles the interaction with the device's microphone via the Android `MediaRecorder` API.
    - **`TranscriptionRemoteSource`**: An interface for transcribing audio. The implementation uses a client (e.g., OkHttp) to communicate with a remote transcription service like OpenAI.
    - **`ConfigProvider`**: An interface for providing configuration for the SDK. The `LocalConfigProviderImpl` reads configuration from a local JSON file, but a remote provider could be easily implemented.

## Data and State Flow

1.  **Initiation**: The client app calls `VoiceReviewer.start()`.
2.  **State Change -> Recording**: The `VoiceReviewer` transitions its state to `Recording` and starts the `AudioCaptureSource`.
3.  **Progress Update**: The `AudioCaptureSource` provides recording progress, which the `VoiceReviewer` emits through the `Recording` state.
4.  **State Change -> Processing**: Once recording is complete, the `VoiceReviewer` transitions to the `Processing` state. It retrieves the audio file and passes it to the `VoiceReviewRepository`.
5.  **Transcription**: The repository uses the `TranscriptionRemoteSource` to upload the audio file and get the transcription.
6.  **State Change -> Success/Error**: Upon receiving a result from the repository, the `VoiceReviewer` transitions to either the `Success` state (with the transcription text) or the `Error` state (with an error message).
7.  **State Change -> Idle**: After a `Success` or `Error` state, the `VoiceReviewer` transitions back to `Idle`, ready for a new review.

This unidirectional data flow, combined with a reactive state machine, makes the SDK robust, predictable, and easy to debug.
