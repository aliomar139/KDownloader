# Contributing to KDownloader

Thanks for helping improve KDownloader.

## Before opening an issue

- Confirm the problem still occurs on the latest `main` branch.
- For extractor failures, include the source website, a public example URL when safe to share, the
  Android version, and the complete error shown by KDownloader.
- Remove cookies, tokens, private URLs, usernames, and other personal information from logs.
- Check whether the same URL currently works with upstream
  [yt-dlp](https://github.com/yt-dlp/yt-dlp).

## Development setup

KDownloader requires JDK 17 and Android SDK 35. Open the repository in Android Studio or configure
`local.properties`, then use the Gradle wrapper.

Before submitting a pull request, run:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew verifyBundledYtDlp
```

Use `gradlew.bat` on Windows.

## Pull requests

- Create a focused branch from `main`.
- Keep changes scoped to one problem.
- Add or update tests for behavior changes where practical.
- Explain user-visible changes and any compatibility tradeoffs.
- Do not commit build output, IDE state, local SDK paths, cookies, credentials, or downloaded media.
- Keep `main` protected; changes should be merged through a pull request.

## Coding conventions

- Application source is Java 17 and Android Views; do not reintroduce Kotlin or Compose without an
  explicit project-level decision.
- Reuse the existing Material theme tokens and 4/8dp spacing rhythm.
- Preserve 48dp Android touch targets and content descriptions for icon-only controls.
- Keep network and media processing off the main thread.
- Prefer explicit error messages over silent fallbacks.

## Legal

By contributing, you agree that your contribution may be distributed under the repository's MIT
License. Do not submit code or media you do not have permission to redistribute.
