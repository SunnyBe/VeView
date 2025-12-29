# Testing Strategy

A robust testing strategy is crucial for the stability and reliability of the VeView SDK. This document outlines our approach to testing.

## Guiding Principles

- **Testability First**: Code should be written in a way that is easy to test. This is achieved through dependency injection, abstraction of external dependencies, and clear separation of concerns.
- **Fast and Reliable Tests**: Tests should be fast and deterministic. Flaky tests are discouraged and should be fixed or removed.
- **Right Tool for the Job**: We use a combination of unit, integration, and (in the future) end-to-end tests to cover different aspects of the SDK.

## Types of Tests

### 1. Unit Tests

- **Location**: `veview-sdk/src/test/java`
- **Purpose**: To test individual components (classes, functions) in isolation.
- **Frameworks**: [JUnit 5](https://junit.org/junit5/), [MockK](https://mockk.io/)

Unit tests are the foundation of our testing pyramid. They are fast, easy to write, and provide quick feedback. We use MockK to create test doubles (mocks, fakes) for dependencies, allowing us to test a single unit of work in isolation.

**Example**: Testing a use case in the `domain` layer by mocking the repository it depends on.

**To run unit tests:**
```bash
./gradlew :veview-sdk:test
```

### 2. Integration Tests

- **Location**: `veview-sdk/src/test/java` (within specific integration test suites)
- **Purpose**: To test the interaction between multiple components of the SDK.

Integration tests verify that different parts of the SDK work together as expected. For example, an integration test might cover the flow from the `VoiceReviewer` down to the `VoiceReviewRepository` and fake data sources.

**Example**: Testing the flow of starting a review, which involves the `VoiceReviewer`, a use case, and the repository, using a fake implementation of the data sources.

### 3. Android Instrumented Tests

- **Location**: `veview-sdk/src/androidTest/java`
- **Purpose**: To test components that depend on the Android framework or hardware, requiring an emulator or physical device.
- **Frameworks**: [AndroidX Test](https://developer.android.com/testing), [Espresso](https://developer.android.com/training/testing/espresso)

These tests are used for parts of the SDK that cannot be tested in a pure JVM environment. This includes:

-   The `WavAudioCaptureSource` which interacts with the `MediaRecorder` API.
-   Permission handling logic.

Due to their slower nature, we keep the number of instrumented tests to a minimum, preferring unit tests wherever possible.

**To run instrumented tests:**
```bash
./gradlew :veview-sdk:connectedAndroidTest
```

## Test Coverage

While we don't enforce a strict code coverage percentage, we encourage developers to write tests for all new features and bug fixes. The goal is to have confidence that our code works as expected, not to hit an arbitrary number.

Run the following command to generate a test coverage report:

```bash
./gradlew :veview-sdk:koverHtmlReport
```

The report will be available in `veview-sdk/build/reports/kover/html/`.
