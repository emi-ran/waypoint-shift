<div align="center">

# 📍 Waypoint Shift

**A modern, transparent, and rootless Android mock-location application built with Jetpack Compose & Material 3.**

[![Language: English](https://img.shields.io/badge/Language-English-blue.svg)](README.md)
[![Dil: Türkçe](https://img.shields.io/badge/Dil-T%C3%BCrk%C3%A7e-red.svg)](README.tr.md)
[![Android](https://img.shields.io/badge/Android-API%2023--36-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Android CI](https://github.com/emi-ran/waypoint-shift/actions/workflows/ci.yml/badge.svg)](https://github.com/emi-ran/waypoint-shift/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![OpenStreetMap](https://img.shields.io/badge/Maps-OpenStreetMap%20%2F%20osmdroid-7EBC6F.svg?logo=openstreetmap&logoColor=white)](https://www.openstreetmap.org)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Local%20%26%20Zero%20Tracking-brightgreen.svg)](#privacy--ethics)

<p align="center">
  <a href="#key-features">Key Features</a> •
  <a href="#device-setup-quick-start">Quick Start</a> •
  <a href="#how-it-works">How It Works</a> •
  <a href="#building-and-development">Building</a> •
  <a href="#privacy--ethics">Privacy</a> •
  <a href="#tech-stack">Tech Stack</a>
</p>

---

</div>

## 📖 Overview

**Waypoint Shift** is a developer-friendly, privacy-first mock-location app for Android. It enables developers, testers, and power users to simulate GPS coordinates reliably without rooting the device or using invasive hooks.

Unlike many legacy spoofing tools, Waypoint Shift is built entirely around **transparency, modern Android standards, and user control**:
- **No root, no hooks, no hacks**: Uses strictly official Android and Google Play Services Location APIs.
- **Transparent by design**: It never hides mock status, never tampers with `Location.isMock()`, and never circumvents system detection.
- **Privacy-first**: Zero telemetry, zero analytics, zero ads, and zero cloud backends. Your coordinates never leave your device.
- **Zero API Keys required**: Powered by OpenStreetMap (osmdroid) and Nominatim address search out-of-the-box.

---

## ✨ Key Features

- 🗺️ **Interactive OpenStreetMap Interface**: Tap anywhere on the map or drag-and-drop the location pin to select coordinates intuitively.
- 🔍 **Address & Place Search**: Instant search for cities, streets, and landmarks via built-in OpenStreetMap Nominatim integration.
- 🎯 **High-Precision Manual Input**: Direct numeric entry for latitude and longitude with canonical 6-decimal validation (`0.000001°` precision ~11 cm).
- ⚡ **Dual Mock Location Engine**:
  - Primary: Google Play Services `FusedLocationProviderClient`.
  - Fallback: Native Android `LocationManager` provider (for AOSP, de-Googled ROMs, or emulators without Play Services).
- 🔔 **Ongoing Foreground Service**: Real-time 1-second location publication managed through a persistent notification displaying active coordinates with a one-tap **Stop** action.
- ⭐ **Saved Favorites & History**:
  - Save custom locations with labels (up to 60 characters).
  - Automatically keeps an ordered, deduplicated history of recent coordinates (up to 20 items).
  - Backed by Jetpack DataStore Preferences (excluded from cloud backups).
- 🛡️ **Actionable System Diagnostics**: Clear in-app checks for Developer Options, Mock Location AppOp status, Google Play services availability, and Android 13+ notification permissions.

---

## 🚀 Device Setup & Quick Start

To use Waypoint Shift on your Android device or emulator:

### 1. Enable Developer Options
1. Open your device's **Settings**.
2. Go to **About Phone** (or *System > About Phone*).
3. Tap **Build Number** 7 times until you see the *"You are now a developer!"* message.

### 2. Select Mock Location App
1. Go back to **Settings** > **System** > **Developer Options** (or search for *Developer Options*).
2. Scroll down to the **Debugging** section.
3. Tap **Select mock location app**.
4. Choose **Waypoint Shift**.

### 3. Grant Notification Permission (Android 13+)
- On Android 13 (API 33) and above, grant the notification permission when prompted.
- *Why?* Waypoint Shift requires a visible foreground service so mock location state is always transparent and controllable.

### 4. Start Mocking
1. Select a location using the **Map**, search via the **Search Bar**, or enter **Latitude / Longitude** manually.
2. Tap **Start**.
3. You can update coordinates on the fly with **Update** or terminate with **Stop** from either the app or the notification.

---

## 🛠️ Tech Stack & Architecture

Waypoint Shift follows modern Android Architecture Components and clean reactive state management:

| Component | Technology | Description |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern, safe, and expressive |
| **UI Framework** | Jetpack Compose & Material 3 | Declarative UI with Dynamic Color & Dark Mode |
| **Maps & Search** | osmdroid & Nominatim | Open-source map rendering and geocoding (no API key required) |
| **Location Engine** | Google Play Services & Android LocationManager | Fused location with graceful AOSP fallback |
| **Storage** | Jetpack DataStore Preferences | Reactive, thread-safe asynchronous local persistence |
| **Concurrency** | Kotlin Coroutines & StateFlow | Reactive UI updates and non-blocking background loops |
| **Service** | Android `specialUse` Foreground Service | Transparent ongoing background publication |

---

## 💻 Building and Development

### Prerequisites
- **JDK 17**
- **Android Studio Ladybug (or newer)** or Android SDK Command-line Tools
- **Android SDK Platform 36** and **Build-Tools 36.0.0**

### Clone & Open
```bash
git clone https://github.com/emi-ran/waypoint-shift.git
cd waypoint-shift
```

### Build via Command Line
Run the Gradle wrapper:

```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows (PowerShell / Command Prompt)
.\gradlew.bat assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests & Lint
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run specific suite
./gradlew :app:testDebugUnitTest --tests '*MainViewModelTest'
./gradlew :app:testDebugUnitTest --tests '*MockLocationPublisherTest'
./gradlew :app:testDebugUnitTest --tests '*LocationRepositoryTest'

# Run lint
./gradlew lintDebug
```

### Continuous Integration

GitHub Actions runs unit tests, Android lint, and a debug APK build on pushes and pull requests to `main`, or manually. It uses Ubuntu 24.04 with JDK 17 and installs SDK Platform 36 / Build-Tools 36.0.0; SDK setup and lint errors fail the job.

Successful runs provide `waypoint-shift-debug-apk` (14-day retention). Test and lint reports are uploaded as `test-and-lint-reports` whenever available (7-day retention), including failed runs. This workflow does not publish releases or deploy the app; device/instrumentation testing remains separate.

---

## 🔒 Privacy & Ethics

Waypoint Shift is designed according to strict ethical and privacy guidelines:

- **No Real Location Access**: Does not request fine, coarse, or background location permissions to track your true whereabouts.
- **Zero Network Tracking**: Contains no analytics SDKs, crash trackers, telemetry libraries, or ad networks.
- **Local Data Isolation**: Coordinates and favorites are stored locally in private application storage and excluded from Android Cloud Backup (`allowBackup="false"`).
- **System Compliance**: Never alters system settings autonomously, never suppresses mock flags, and honors Android system lifecycles.

---

## 📋 System Requirements & Compatibility

- **Minimum Supported OS**: Android 6.0 Marshmallow (API level 23)
- **Target OS**: Android 16 (API level 36)
- **Root Status**: Not required (Works on unrooted stock devices and custom ROMs)
- **Google Play Services**: Optional (Falls back to native system LocationManager when Play Services are absent)

---

## 📄 License & Documentation

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).

Detailed architecture specifications, TDD test plans, and release criteria can be found in:
[`docs/plans/2026-08-24-waypoint-shift.md`](docs/plans/2026-08-24-waypoint-shift.md)

Project changes and version history are documented in [`CHANGELOG.md`](CHANGELOG.md).
