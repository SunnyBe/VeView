# Changelog

All notable changes to this project will be documented in this file.

## [1.0.3] - 2025.01.06

### Added
- **Customizable Analysis Response**: Introduced a major new feature allowing clients to define their own data structure for the analysis result.
  - Added a new generic method, `newCustomAudioReviewer<T>()`, which takes a client-defined data class (`Class<T>`) and returns a `VoiceReviewer<T>`.
  - Introduced the `@FieldDescription` annotation, which allows clients to describe the fields in their custom data class. These descriptions are used to dynamically generate a precise prompt for the AI, ensuring an accurate and structured JSON response.

### Changed
- The core `AnalysisEngine` and `VoiceReviewer` interfaces are now generic (`AnalysisEngine<T>`, `VoiceReviewer<T>`) to support the new customizable analysis feature.
- The `VoiceReviewState.Success` class is now also generic (`Success<T>`) and will contain the client-defined data model upon a successful analysis.

## [1.0.1.04] - 2025.01.04

### Fixed
- Resolved a critical reflection crash (`KotlinReflectionInternalError`) in release builds by integrating the KSP annotation processor and removing default arguments from the `GptAnalysisResponse` data class. This ensures compatibility with code shrinkers like ProGuard.

### Changed
- Reduced excessive logging in the `OpenAIAnalysisEngine` to provide a cleaner and more focused output.

## [1.0.1] - 2025.01.04

### Added
- Comprehensive logging within the `OpenAIAnalysisEngine` to provide deeper insights into the transcription and analysis process.

### Fixed
- Resolved a critical bug causing JSON parsing failures from the OpenAI service by refining the internal `GptAnalysisResponse` data model to handle field mismatches gracefully.

### Changed
- Improved error propagation within the analysis engine by wrapping failures in a specific `AnalysisFailedException` for clearer debugging context.

## [1.0.0] - 2025.01.03

### Added

- Developer-facing error messages to help clients log and report issues.

### Fixed

- The SDK now performs a runtime check to ensure the API key is not blank or empty. If the key is missing, an `IllegalStateException` is thrown to prevent unexpected behavior.

### Changed

- Removed unused code and dependencies to streamline the SDK and improve performance.

## [0.0.7] - 2025-01-01

### Added

- Initial release of the VeView SDK.
- Core functionality for recording voice reviews and managing the lifecycle through a reactive `StateFlow`.
- Integration with OpenAI for AI-powered audio transcription.
