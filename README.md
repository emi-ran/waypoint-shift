# Waypoint Shift

Waypoint Shift is an Android mock-location tool built around official Android and Google Play services APIs. It will remain transparent: mock state must stay visible to Android and other apps, mocking must start only after explicit user action, and normal/error stop paths must disable mock mode.

## Current scope

Repository contains Phase 0, Phase 1, and Phase 2 foundation:

- Android API 36 app bootstrap with Kotlin, Jetpack Compose, and Material 3
- Gradle 9.5.0 wrapper with pinned distribution checksum
- Local light/dark Compose theme and placeholder launch screen
- Validated `GeoPoint` domain model with locale-independent signed decimal parsing, including trailing decimal points, and six-decimal formatting
- Local favorites and recents persistence via Preferences DataStore and Kotlin Serialization
  - Explicit v1 disk DTOs ignoring unknown fields
  - Graceful recovery from malformed JSON without crashing startup
  - Favorites CRUD with stable-ID uniqueness and 60-code-point label truncation
  - Recents deduplicated by 6-decimal normalized coordinates, newest-first, capped at 20 entries
- Unit and integration tests covering domain models, serialization, and DataStore persistence

Map UI, mock-location publication, and foreground service are not implemented yet.

## Requirements

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Android SDK path available through `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or ignored `local.properties`

## Local configuration

Copy example file and set machine-specific Android SDK path:

```bash
cp local.properties.example local.properties
```

`MAPS_API_KEY` may remain placeholder for current debug foundation builds. Before map work or release builds, replace it with Android-restricted Google Maps SDK key. Never commit `local.properties` or real key.

## Build and test

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Focused coordinate tests:

```bash
./gradlew :app:testDebugUnitTest --tests '*GeoPointTest'
```

Debug APK appears under `app/build/outputs/apk/debug/`.

## Product boundaries

- No root, hooks, hidden APIs, mock-state concealment, or developer-setting changes
- No real/background location permission unless later requirements prove it necessary
- No tracking, ads, accounts, analytics, telemetry, or backend service
- No automatic mocking after process death, reboot, or app launch

See [`docs/plans/2026-08-24-waypoint-shift.md`](docs/plans/2026-08-24-waypoint-shift.md) for phased implementation plan.
