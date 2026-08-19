# Kotlin to Java Migration Notes

## Phase 0 — Kotlin baseline

- Branch: `java-migration`, based on tag `kotlin-final` at `450ef8b`.
- Baseline command: `.\gradlew.bat --no-daemon clean :app:assembleDebug :app:testDebugUnitTest`.
- Result: passed (`assembleDebug`, `testDebugUnitTest`, and the `verifyBundledYtDlp` pre-build dependency).
- Unit tests: 65 passed, 0 failed, 0 errors, 0 skipped across 11 test classes.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 171,217,339 bytes (163.29 MiB).
- Method references: 180,748 across 12 DEX files (`apkanalyzer dex references`).
- Native-strip warning: the baseline build cannot strip the packaged `libffmpeg.zip.so` and `libpython.zip.so` payloads; Gradle packages them unchanged and the build succeeds.
- Connected-device and manual parity gates: skipped because `adb devices -l` reported no connected device or emulator.

## Approved deviations

None yet.

## Kotlin-library Java interop adaptations

None yet.

## Pre-existing issues not fixed here

- No device was available to identify runtime-only baseline issues.
