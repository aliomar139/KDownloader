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

## Phase 1 — Groovy build scripts

- Replaced `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts` with Groovy DSL equivalents.
- Kotlin Android, Compose, and KSP plugins and all dependencies remain intentionally enabled for mixed-source migration phases.
- Gates: `assembleDebug`, `testDebugUnitTest` (65 passed), and `verifyBundledYtDlp` passed before and after deleting the `.kts` scripts.
- Connected-device and manual gates: skipped because no device or emulator is connected.

## Phase 2 — Java and Views foundations

- Added the approved Android Views dependency set needed by later screen phases; Room remains on KSP until phase 4 as required.
- Added `AppExecutors` and `StateHolder` as the Java executor and observable-state foundations.
- Reparented `Theme.KDownloader` to Material 3 and ported the Compose light/dark palette, high-contrast overlay, typography, and shape sizes to resources.
- Added 35 vector drawables from Google's official Material icon source, matching the filled/outlined variants used by the Compose UI.
- Extracted 365 distinct static UI texts into resources (366 total strings including `app_name`) and replaced 246 direct Compose text/title/subtitle/accessibility call sites.
- Approved deviation: UI string literals moved to `strings.xml` with identical text. Indirect model labels and formatted strings remain represented by their existing Kotlin values until their owning Java/View phase converts the call site.
- Gates: clean `assembleDebug` and `testDebugUnitTest` passed (65 tests, 0 failures); `verifyBundledYtDlp` ran through `preBuild`.
- Connected-device and manual visual-parity gates: skipped because no device or emulator is connected.

## Phase 3 — Leaf logic to Java

- Replaced all 18 scoped Kotlin leaf files with Java: six utilities, three settings stores, five settings primitives, and four engine helpers.
- Ported the 10 specified JVM test classes to Java with all 54 test methods retained; the full suite remains 65 tests.
- Replaced coroutine-backed leaf operations with blocking `@WorkerThread` methods. Existing Compose callers explicitly dispatch scanner and thumbnail work to `Dispatchers.IO` until their owning screens are migrated.
- Replaced settings change `Flow` at the store boundary with lifecycle-aware `LiveData`; `SettingsRepository` contains the temporary Flow bridge until Phase 5.
- Added blocking Room DAO helpers for scanner insertion and URI reads. They reuse the existing SQL/insert contract and will become the primary Java DAO methods in Phase 4.
- Kotlin data-class conveniences at mixed-language call sites were replaced with explicit Java constructors and `withDirect` / `withFormatSelector` methods.
- Gates: `assembleDebug` and `testDebugUnitTest` passed; 65 tests, zero failures.
- Connected-device and manual gates: skipped because no device or emulator is connected.

## Phase 4 — Settings models and Room data

- Split `SettingsModel.kt` into one Java type per file: 19 `SettingOption` enums, `SettingsCategory`, the shared option resolver, and 10 immutable settings snapshot classes.
- Preserved every enum storage key, label, default value, preference key, clamping rule, and grouped-settings equality contract. Kotlin `copy` call sites now use explicit `withX` methods.
- Converted `DownloadEntity`, `DownloadStatus`, `Converters`, `DownloadDao`, and `AppDatabase` to Java without changing the `downloads` table, columns, type conversion, database filename, or schema version 1.
- Replaced Room `Flow`/suspend DAO methods with `LiveData` plus blocking worker-thread methods and `getAllSync()` for tests. The Compose history screen uses a temporary lifecycle-aware LiveData bridge and dispatches writes to `Dispatchers.IO`.
- Replaced KSP Room processing with `annotationProcessor androidx.room:room-compiler:2.6.1` and removed the KSP plugin.
- Ported `SettingsRepositoryTest`, `DownloadDaoTest`, and `DownloadDirectoryScannerAndroidTest` to Java. The instrumentation APK compiles successfully.
- Gates: `assembleDebug`, `assembleDebugAndroidTest`, and `testDebugUnitTest` passed; 65 JVM tests, zero failures.
- Connected instrumentation, installed-data compatibility, and manual gates: skipped because no device or emulator is connected. A later `adb devices -l` check timed out while starting/querying adb; no device became available.

## Kotlin-library Java interop adaptations

None yet.

## Pre-existing issues not fixed here

- No device was available to identify runtime-only baseline issues.
