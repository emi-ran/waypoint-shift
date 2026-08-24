# Waypoint Shift

Waypoint Shift is a transparent, no-root Android mock-location tool using official Android and Google Play services APIs. It never hides mock state, changes Developer Options, reads real device location, or starts mocking without explicit user action.

## Features

- Map tap and draggable-marker coordinate selection
- Validated manual latitude/longitude entry with canonical six-decimal formatting
- Explicit Start, Update, and Stop controls
- One-second fused mock-location publication through a user-visible foreground service
- Ongoing notification showing exact active coordinate and Stop action
- Local favorites with optional labels plus bounded, deduplicated recents
- Developer Options onboarding and actionable mock-app selection failure flow
- Android 13+ notification permission gate before mocking starts
- Local coordinate storage excluded from Android backup

## Requirements

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Android SDK path through `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or ignored `local.properties`
- Google Play services device/emulator for mock publication
- Android-restricted Google Maps SDK key for map tiles

## Local setup

Copy local configuration template:

```bash
cp local.properties.example local.properties
```

Set local values without committing file:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
MAPS_API_KEY=YOUR_ANDROID_RESTRICTED_KEY
```

Restrict Maps key to package `com.waypointshift`, expected signing certificate fingerprints, and Maps SDK for Android. Debug build accepts placeholder key but map tiles will not render. Release build rejects absent or placeholder key.

## Device setup

1. Enable Android Developer Options.
2. Open **Select mock location app**.
3. Select **Waypoint Shift**.
4. On Android 13+, grant notification permission when prompted. App blocks Start when permission is denied so active mocking remains visible.
5. Select coordinate through map or manual fields, then tap **Start**.

App opens public Developer Options screen but never reads or changes secure settings. Successful Google Play services mock-mode call determines whether setup is valid.

## Build and test

```bash
JAVA_HOME=/home/hermes/android-toolchain/jdk17 \
ANDROID_HOME=/home/hermes/Android/Sdk \
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Focused suites:

```bash
./gradlew :app:testDebugUnitTest --tests '*LocationRepositoryTest'
./gradlew :app:testDebugUnitTest --tests '*MockLocationPublisherTest'
./gradlew :app:testDebugUnitTest --tests '*MainViewModelTest'
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Privacy

- Stores only user-selected coordinates, optional favorite labels, and local timestamps.
- No actual-location access, tracking, ads, analytics, telemetry, accounts, backend, or cloud sync.
- No fine, coarse, or background location permission.
- Favorites and recents remain in Preferences DataStore on device and are excluded from backup.
- Maps SDK loads map tiles from Google under Google Maps Platform terms; manual coordinate workflow remains available when map tiles fail.

## Limits

- Android, OEM, or user force-stop can terminate foreground service. App never auto-restarts mocking after process death, force-stop, reboot, or launch.
- Cleanup is best effort after abrupt process/device termination because Android cannot guarantee callbacks.
- `specialUse` foreground-service classification can require store declaration and review.
- Full release acceptance still needs API 23/API 36 device smoke tests, notification/service lifecycle tests, TalkBack/large-font/landscape checks, and restricted-key map verification.

See [`docs/plans/2026-08-24-waypoint-shift.md`](docs/plans/2026-08-24-waypoint-shift.md) for architecture, boundaries, phased TDD plan, and release gates.
