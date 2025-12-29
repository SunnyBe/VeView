# Contribution Guidelines

Thank you for considering contributing to the VeView SDK! We appreciate your help in making this project better. Please take a moment to review this document to ensure a smooth and effective contribution process.

## Code of Conduct

This project and everyone participating in it are governed by our [Code of Conduct](<fill content manually: link to CODE_OF_CONDUCT.md>). By participating, you are expected to uphold this code. Please report unacceptable behavior.

## How to Contribute

### Reporting Bugs

- **Ensure the bug was not already reported** by searching the GitHub Issues.
- If you're unable to find an open issue addressing the problem, [open a new one](<fill content manually: link to new issue page>). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample or an executable test case** demonstrating the expected behavior that is not occurring.

### Suggesting Enhancements

- Open a new issue with the `enhancement` label.
- Clearly describe the proposed enhancement and the motivation for it.
- Provide examples of how the new feature would be used.

### Submitting Pull Requests

1.  Fork the repository and create your branch from `main`.
2.  If you've added code that should be tested, add tests.
3.  Ensure the test suite passes (`./gradlew :veview-sdk:test`).
4.  Make sure your code lints (`./gradlew :veview-sdk:lint`).
5.  Follow the [Coding Style](#coding-style) and [Commit Message Format](#commit-message-format).
6.  Issue that pull request!

## Coding Style

We generally follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android's best practices](https://developer.android.com/kotlin/style-guide). The project is set up with `ktlint` to enforce a consistent style. You can run the linter with:

```bash
./gradlew :veview-sdk:lint
```

Before submitting a pull request, please ensure there are no linting errors.

## Commit Message Format

We follow a conventional commit message format to keep the history clean and readable.

**Format:** `<type>(<scope>): <subject>`

- **`type`**: The type of change (e.g., `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`).
- **`scope`** (optional): The part of the codebase affected (e.g., `audiocapture`, `core`, `build`).
- **`subject`**: A concise description of the change.

**Examples:**

- `feat(audiocapture): Add support for custom audio bitrates`
- `fix: Correctly handle microphone permission denial`
- `docs: Update README with new configuration options`
- `refactor(core): Simplify state management logic`

## Branch Naming Convention

Please use the following format for your branch names:

`<type>/<short-description>`

- **`type`**: `feature`, `fix`, `refactor`, `docs`, etc.

**Examples:**

- `feature/add-remote-config`
- `fix/audio-recording-bug`
