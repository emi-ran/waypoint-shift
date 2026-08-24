# Waypoint Shift

Waypoint Shift is an Android mock-location tool built around official Android and Google Play services APIs. It will remain transparent: mock state must stay visible to Android and other apps, mocking must start only after explicit user action, and normal/error stop paths must disable mock mode.

## Current scope

Repository currently contains Phase 0/1 foundation only:

- Android API 36 app bootstrap with Kotlin, Jetpack Compose, and Material 3
- Gradle 9.5.0 wrapper with pinned distribution checksum
- Local light/dark Compose theme and placeholder launch screen
- Validated `GeoPoint` domain model with locale-independent parsing and six-decimal formatting
- Unit tests for coordinate boundaries, malformed text, and non-finite values
- Backup exclusions prepared for future local coordinate preferences

Map UI, favorites, mock-location publication, and foreground service are not implemented yet.

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
