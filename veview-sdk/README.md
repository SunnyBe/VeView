# VeView SDK Developer Guide

Welcome to the developer guide for the VeView SDK! This document is intended for engineers who are actively working on and contributing to the SDK.

For instructions on how to *use* the SDK in an application, please refer to the root `README.md` file of this project.

## 🚀 Getting Started for Contributors

### 1. Environment Setup
- Clone the repository.
- Open the project in a recent version of Android Studio.
- The project is self-contained and should sync with Gradle without any additional setup.

### 2. Dependency Management
This project uses Gradle's [dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html) to ensure repeatable builds. After cloning, or if you need to update dependencies, run the following script to generate the lockfiles:

```bash
./generate_dependency_locks.sh generate
```
Commit any changes to the `*.lockfile` files to version control.

### 3. Building the SDK
You can build the SDK from Android Studio or by running the following Gradle command from the project root:

```bash
./gradlew :veview-sdk:assembleRelease
```
The resulting AAR file will be located in `veview-sdk/build/outputs/aar/`.

## 📚 Documentation
To keep our documentation organized and easy to navigate, we've split it into several files:

- **[Architecture Overview](ARCHITECTURE.md)**: A deep dive into the SDK's architecture, key components, and design patterns.
- **[Contribution Guidelines](CONTRIBUTING.md)**: Our coding standards, commit message format, and the process for submitting pull requests.
- **[Testing Strategy](TESTING.md)**: Information on how to write and run unit and integration tests for the SDK.

## 🛠️ Project Structure
The SDK follows a standard Android library structure and is organized by feature and layer:

- `com.veview.veviewsdk`
    - `data/`: Contains data sources, repositories, and models. This layer handles all I/O, including network requests to the transcription service and local file storage.
        - `audiocapture/`: Classes responsible for raw audio capture.
        - `configs/`: Logic for loading local or remote configuration.
        - `openai/`: Network client for interacting with the OpenAI API.
    - `domain/`: Contains the core business logic, use cases, and entities. This layer is pure Kotlin and independent of the Android framework.
    - `di/`: Dependency injection setup using a manual, simplified approach for broader compatibility.
    - `core/`: The main entry point `VoiceReviewer` and the `VoiceReviewState` machine.

## ✨ Key Principles
- **Decoupling**: The SDK is designed to be highly decoupled. Business logic is separated from the Android framework, and hardware interactions (like microphone access) are abstracted away.
- **Testability**: All components are designed to be easily testable. See the [Testing Strategy](TESTING.md) for more details.
- **Asynchronous by Default**: The SDK uses Kotlin Coroutines and Flow for all asynchronous operations, ensuring a non-blocking and responsive experience.

## 🤝 How to Contribute
We welcome contributions! Please read our [Contribution Guidelines](CONTRIBUTING.md) to get started.
