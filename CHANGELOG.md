# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
