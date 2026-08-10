<div align="center">

<img src="app/src/main/logo/android-playstore-512x512.png" alt="KDownloader" width="128" height="128">

# KDownloader

**A modern, fully offline media downloader for Android — powered by yt-dlp, built with Jetpack Compose.**

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-24-blue?style=flat-square)](https://developer.android.com/tools/releases/platforms)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square)](https://developer.android.com/jetpack/compose)
[![yt-dlp](https://img.shields.io/badge/yt--dlp-bundled-FF0000?style=flat-square&logo=youtube&logoColor=white)](https://github.com/yt-dlp/yt-dlp)

</div>

---

## Overview

KDownloader wraps the full power of [yt-dlp](https://github.com/yt-dlp/yt-dlp) and
[FFmpeg](https://ffmpeg.org) in a native Android app. Paste a link — or share one into the app from
any browser — and KDownloader resolves the available formats, downloads in a foreground service that
survives app switches, and writes the result straight into your device's media library.

No account. No telemetry. No server in the middle. The extractor runs on your phone.

## Features

### Downloading
- **Video and audio** — MP4, WebM, or best-compatible video; MP3, M4A, Opus, or original audio.
- **Quality control** — up to 2160p, with frame-rate preference (30/60 FPS), optional HDR, and
  automatic fallback when a requested rendition isn't offered.
- **Android-friendly codecs** — prefers formats that play natively on device, so files just work in
  Photos, Gallery, and any stock player.
- **Metadata and artwork** — embeds thumbnails and tags, and preserves the original upload date.
- **Subtitles** — manual or auto-generated captions in SRT/VTT, embedded in the video or saved
  alongside it, with per-language filename tagging.

### Queue and reliability
- **Foreground service** with live progress notifications and a cancel action — downloads keep
  running while you use other apps.
- **Configurable concurrency**, retry counts, auto-resume of interrupted transfers, and queue
  restoration after an app restart.
- **Duplicate detection** by source URL, media ID, or existing filename.
- **Device-aware pausing** on battery saver or thermal throttling, plus optional speed limits and
  scheduled download windows.

### Storage
- **Storage Access Framework** folder selection — pick separate destinations for video, audio, and
  temporary files, with readable folder names and permission-revocation checks.
- **Filename templates** with configurable conflict handling (auto-number, replace, skip, or ask)
  and a length cap for restrictive filesystems.
- **Subfolder organization** by channel, playlist, or media type.
- **MediaStore integration** so finished files are immediately visible to the rest of the system.

### Network and privacy
- **Network gating** — Wi-Fi only, Wi-Fi + mobile, or any connection, with roaming controls,
  metered-Wi-Fi handling, and a mobile-data confirmation threshold.
- **HTTP and SOCKS proxy support**, with credentials held in Keystore-backed encrypted storage —
  the password never touches plain preferences.
- **Local history** in a Room database with retention policies (forever, 30 days, 7 days, or
  session-only) and one-tap clearing.

### Interface
- Material 3 with light, dark, and system themes, plus optional dynamic color.
- Compact list mode, toggleable size/speed/ETA readouts, reduced-animation and high-contrast modes.
- Share-target integration: send any `text/plain` link from another app straight to the download
  sheet.

## Screenshots

> _Coming soon._

## Architecture

```
app/src/main/java/com/kira/kdownloader/
├── engine/       yt-dlp orchestration — format selection, extractor quirks, version policy
├── service/      Foreground DownloadService and progress event bus
├── data/         Room database, DAO, and download history entities
├── settings/     Immutable settings model, repository, secure store, and settings UI
├── ui/           Compose screens (home, history), view models, and Material 3 theme
└── util/         MediaStore writing, thumbnails, directory scanning, formatting helpers
```

Design notes worth knowing:

- **Settings are stable by key, not by ordinal.** Every enumerated option persists an explicit
  string key, so reordering or inserting constants can never corrupt a stored preference.
- **The engine layer is pure where it can be.** Format selection, filename templating, proxy
  validation, and output-path resolution are side-effect-free and unit-tested independently of
  Android.
- **yt-dlp is pinned and verified.** The app ships a specific yt-dlp build whose SHA-256 is checked
  by a Gradle task wired into `preBuild` — a tampered or truncated binary fails the build rather
  than the download. At runtime, an opportunistic background self-update (throttled to once per
  day) keeps the extractor current without ever delaying a download.

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.0.20 |
| UI | Jetpack Compose (BOM 2024.09.02), Material 3 |
| Persistence | Room 2.6.1 with KSP |
| Media engine | `youtubedl-android` 0.18.1 + bundled FFmpeg |
| Images | Coil 2.7.0 for Compose |
| Security | `androidx.security:security-crypto` (Keystore-backed) |
| Build | Android Gradle Plugin 8.7.3, Java 17 target |

Native ABIs: `armeabi-v7a`, `arm64-v8a`, `x86_64`.

## Building

**Requirements**
- Android Studio Ladybug or newer (or a standalone JDK 17 + Android SDK 35)
- Android SDK Platform 35 and NDK-capable build tools

**Steps**

```bash
git clone https://github.com/aliomar139/KDownloader.git
cd KDownloader
```

Point the build at your SDK by creating `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

Then build or install:

```bash
./gradlew assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew installDebug           # build and push to a connected device
```

**Tests**

```bash
./gradlew test                   # JVM unit tests (engine, settings, utils)
./gradlew connectedAndroidTest   # instrumented tests (Room DAO, directory scanner)
./gradlew verifyBundledYtDlp     # verify the bundled extractor checksum
```

## Permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | Fetch media metadata and download files |
| `POST_NOTIFICATIONS` | Show download progress and completion |
| `FOREGROUND_SERVICE`, `..._DATA_SYNC` | Keep downloads alive in the background |
| `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` | Read back finished files for previews (API 33+) |
| `READ_EXTERNAL_STORAGE` (≤32), `WRITE_EXTERNAL_STORAGE` (≤28) | Legacy storage access on older releases |

## Legal

KDownloader is a client for yt-dlp and does not host, index, or distribute any media. You are
responsible for ensuring your use complies with the terms of service of the sites you access and
with the copyright law in your jurisdiction. Download only content you own or have permission to
download.

## Acknowledgements

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) — the extractor that makes this possible
- [youtubedl-android](https://github.com/JunkFood02/youtubedl-android) — Android bindings for yt-dlp and FFmpeg
- [FFmpeg](https://ffmpeg.org) — media muxing, remuxing, and conversion

## License

Released under the [MIT License](LICENSE).
