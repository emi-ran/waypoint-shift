# Waypoint Shift Android Implementation Plan

Date: 2026-08-24  
Status: implementation-ready plan; no production code exists yet

## 1. Goal and hard boundaries

Build a production-quality, single-device Android app that lets a user explicitly choose a coordinate and publish it through Android's supported mock-location path. App remains transparent: Android and other apps must continue to see locations as mock, persistent notification must identify active mocking, and every reachable stop/cleanup path must disable Fused Location Provider mock mode.

Required product behavior:

- Kotlin, Jetpack Compose, Material 3, Coroutines, `StateFlow`, ViewModel, and Preferences DataStore.
- Google Maps SDK for Android through Maps Compose.
- Google Play services `FusedLocationProviderClient` for `setMockMode` and `setMockLocation`.
- Map tap, draggable marker, validated manual latitude/longitude entry, explicit Start, explicit Stop.
- Local favorites and bounded recent locations. No cloud sync.
- Developer Options onboarding that opens system settings but never changes them.
- User-visible foreground service while publishing one mock location repeatedly.
- No root, hooks, hidden APIs, concealment, `Location.isMock()` bypass, developer-setting changes, ads, accounts, analytics, telemetry, backend, or background-location permission.

Non-goals for first release:

- Routes, joystick movement, GPX/KML import, location search/geocoding, device-current-location display, scheduled starts, boot starts, widgets, Wear OS, or multi-device sync.
- Places SDK and Places API. Map tiles are enough; this avoids another billable API and data flow.
- Automatic verification of selected mock-location app through private settings. Successful `setMockMode(true)` on explicit Start is authoritative.

## 2. Repository and naming assumptions

Repository currently contains only `AGENTS.md`; no Gradle project or README exists. Bootstrap a single `app` module.

- Gradle root name: `WaypointShift`
- Application ID and Kotlin package: `com.waypointshift`
- Package source root: `app/src/main/java/com/waypointshift/`
- Minimum supported Android version: API 23, matching current Maps Compose baseline.
- Compile SDK: API 36.
- Target SDK: API 36.
- Version code/name start: `1` / `0.1.0`.
- One activity: `MainActivity`.
- One foreground service: `MockLocationService`.
- One process only. No backend, database, worker scheduler, receiver, or boot component.

If product owner wants another application ID, decide before first release signing or API-key restriction. Changing it later affects installs, Play identity, mock-app selection, and Maps key restrictions.

## 3. Toolchain requirements and version policy

Install before implementation:

- Current stable Android Studio release supporting Android API 36 and target SDK 36.
- JDK 17, selected by `JAVA_HOME` and Gradle JVM.
- Android SDK Platform 36, latest stable API 36 Build Tools, Platform Tools, and command-line tools.
- API 36 Google APIs emulator image with Google Play services, plus one physical Android 16/API 36 device for release validation.
- Git and Android Debug Bridge (`adb`).
- Google Cloud project with Maps SDK for Android enabled and billing configured as required by Google Maps Platform.

Version rules:

1. At implementation start, read official compatibility tables and select latest stable, mutually compatible Android Gradle Plugin, Gradle wrapper, Kotlin, Compose BOM, AndroidX, Maps Compose, Play Services Location, Coroutines, DataStore, Kotlin Serialization, and Secrets Gradle Plugin versions.
2. Do not copy preview, alpha, beta, release-candidate, or dynamic `+` versions into production unless stable API 36 support is impossible and owner explicitly approves.
3. Pin plugins and libraries in `gradle/libs.versions.toml`; pin Gradle in `gradle/wrapper/gradle-wrapper.properties`.
4. Commit generated wrapper JAR/scripts. Do not commit Android SDK paths, API keys, signing keys, or user-local Gradle settings.
5. Use Compose BOM for compatible Compose artifacts; keep Maps Compose explicitly pinned because it is outside Compose BOM.
6. Run `./gradlew dependencyUpdates` only if an approved update plugin is added later; no update plugin is needed for v1.

API 36 assumptions to verify during Phase 0:

- `compileSdk = 36` and `targetSdk = 36` build with selected stable AGP.
- Target-36 behavior changes and Android 16 foreground-service guidance do not add a newer restriction for `specialUse` beyond manifest declaration, user-initiated start, visible notification, and Play declaration/review.
- Edge-to-edge layout, predictive back, notification behavior, and 16 KB page-size compatibility work on API 36. Google Maps/Play Services native dependencies must be current stable versions with supported native binaries.
- Runtime behavior is tested on API 23 and API 36. API 36 is release gate; API 23 is compatibility floor.
- Re-check official Android 16/API 36 release notes on implementation day; this plan must not override newer platform requirements.

Initial environment checks:

```bash
java -version
adb version
sdkmanager --list
git status --short
```

## 4. Architecture

Use small interfaces around Android/Google APIs, a single app container, and unidirectional UI state. Avoid DI framework overhead.

```text
Compose UI
  MainViewModel: StateFlow<MainUiState>
    LocationRepository: Flow<SavedLocations>
      Preferences DataStore + JSON serializer
    MockServiceController
      explicit intents to MockLocationService

MockLocationService
  MockLocationPublisher
    GoogleMockLocationClient
      FusedLocationProviderClient
```

Core contracts:

- `GeoPoint`: validated latitude/longitude value object. Latitude `-90.0..90.0`; longitude `-180.0..180.0`; both finite.
- `SavedLocation`: stable ID, optional trimmed label, `GeoPoint`, creation/last-used timestamp.
- `LocationRepository`: favorite CRUD and recent insertion/deduplication; emits immutable snapshots.
- `MockLocationClient`: suspending `enable()`, `publish(GeoPoint, timestamp)`, and `disable()` methods.
- `MockLocationPublisher`: owns periodic loop, idempotent start/update/stop, and cleanup ordering independent of Play Services implementation.
- `MockServiceController`: UI-safe explicit service commands only.
- `MainUiState`: setup status, selection, manual-input validation, favorite/recent lists, service status, and actionable error. Never treat a persisted flag as proof that Android service is alive.

Concurrency rules:

- Service owns one `SupervisorJob` and one publication `Job`.
- Serialize Start, Update, and Stop commands with a `Mutex` or one command-processing coroutine.
- Repeated Start updates current point without creating a second loop.
- Stop cancels loop, waits for it, calls `setMockMode(false)` in `NonCancellable`, then removes notification and stops service.
- On publish failure, move to failed state, run same disable cleanup, stop service, and expose actionable failure on next app bind/start through a process-local status flow.
- Use `START_NOT_STICKY`; never recreate mocking after process death or reboot without explicit user action.
- `onDestroy()` attempts idempotent cleanup. Android cannot guarantee callbacks after force-stop, process kill, device crash, or power loss; do not claim impossible guarantees. On next app launch, perform a best-effort `setMockMode(false)` reconciliation before allowing a new Start.

## 5. Official mock-location implementation

`GoogleMockLocationClient` wraps `LocationServices.getFusedLocationProviderClient(context)`.

Start sequence, only after visible Activity receives Start click:

1. Validate coordinate.
2. Start foreground service through `ContextCompat.startForegroundService` while app is visible.
3. Service calls `startForeground(...)` immediately in `onStartCommand`, before async setup.
4. Call `fusedLocationProviderClient.setMockMode(true).await()`.
5. Build and publish first `android.location.Location` immediately.
6. Continue publication on fixed one-second interval using monotonic scheduling; avoid unbounded drift or overlapping calls.

Each location must set:

- Provider: `LocationManager.FUSED_PROVIDER` where supported by minimum SDK; otherwise literal official fused provider value only if API availability requires it.
- Latitude and longitude from validated `GeoPoint`.
- `accuracy = 3f` meters as an explicit product default.
- `time = clock.currentTimeMillis()`.
- `elapsedRealtimeNanos = clock.elapsedRealtimeNanos()`.
- Optional altitude, speed, and bearing are omitted/cleared in v1 rather than invented.
- Do not mutate mock flags or attempt concealment. `setMockLocation` provides a mock location by design.

Stop sequence:

1. Cancel and join periodic publication job.
2. Call `setMockMode(false).await()` in `NonCancellable` with bounded timeout and retained error reporting.
3. Clear in-memory selected point and active state.
4. `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelfResult(startId)`.

Map Google API failures to user actions without exposing internals:

- `SecurityException` or permission-denied task: explain app is not selected under Developer Options > Select mock location app; offer Open Developer Options and Retry.
- Google Play services missing/outdated: show repair/update action through `GoogleApiAvailability` where resolvable.
- Other API error: stop safely and show concise retry guidance; retain throwable only in local Logcat, never analytics.

Do not call deprecated `Settings.Secure.ALLOW_MOCK_LOCATION`, read private secure settings, run shell commands from app, or infer success from onboarding checkbox.

## 6. Foreground service and Android 14–16 restrictions

Continuous mock publication needs an ongoing foreground service, but it does not read device location. Declaring `location` service type would trigger while-in-use location permission prerequisites and would falsely describe behavior. Plan assumes `specialUse` is correct service type for this uncategorized, user-visible operation; verify against latest API 36 docs and intended store policy before implementation.

Manifest plan in `app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".service.MockLocationService"
    android:exported="false"
    android:foregroundServiceType="specialUse"
    android:stopWithTask="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Continuously publishes a user-selected mock location while an ongoing notification is visible" />
</service>
```

Restrictions and design responses:

- Start only from direct Start click while `MainActivity` is visible. No background start, alarm, receiver, boot receiver, notification trampoline, or automatic resume.
- Call `startForeground` immediately and comfortably within system deadline.
- Notification channel importance Low, ongoing notification title “Mock location active”, exact coordinates in content, and explicit Stop action handled directly by service.
- Notification content updates when marker/coordinate changes.
- Request `POST_NOTIFICATIONS` on Android 13+ with transparent rationale. If denied, explain foreground service remains visible in system Active apps/Task Manager but drawer visibility is reduced; product decision should prefer requiring permission before Start for transparent behavior.
- Do not request `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, or `ACCESS_BACKGROUND_LOCATION`; v1 never reads real device location.
- `specialUse` can require Google Play Console foreground-service declaration and review. Prepare demo instructions showing direct user start, ongoing notification, and user stop. If store rejects classification, pause release and reassess; do not mislabel as `location`, `dataSync`, or `shortService`.
- No restart after reboot or process death. `START_NOT_STICKY` honors explicit-user-action boundary.
- OEM task killers can terminate service despite FGS. UI and docs must state limitation, and next launch must reconcile stale mock mode best-effort.

## 7. Maps API key and secret handling

- Apply current stable `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`.
- Developer key lives only in ignored root `local.properties` as `MAPS_API_KEY=...`.
- Commit `local.properties.example` with `MAPS_API_KEY=YOUR_ANDROID_RESTRICTED_KEY` and no real value.
- Commit `local.defaults.properties` with an empty or intentionally invalid placeholder so unit tests/configuration can run without a secret; release assembly must fail through a Gradle check when key is absent/placeholder.
- Manifest uses only `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />`.
- Restrict production key in Google Cloud to Android apps with release package `com.waypointshift` and release signing SHA-1/SHA-256 fingerprints; use separate debug key restricted to debug certificate.
- Restrict key API scope to Maps SDK for Android. Configure quota/budget alerts. A mobile API key is extractable from APK, so platform and API restrictions—not obscurity—provide protection.
- Never expose key through `BuildConfig`, UI, logs, tests, screenshots, docs, CI output, or committed Gradle properties unless Maps plugin unavoidably generates it; manifest placeholder is preferred.
- CI injects `MAPS_API_KEY` into temporary `local.properties` or supported secret property immediately before instrumentation/release build and deletes it afterward. Never upload local secret files as artifacts.

## 8. DataStore model

Use one Preferences DataStore instance in `app/src/main/java/com/waypointshift/data/LocationPreferencesDataStore.kt`.

Keys:

- `favorites_json_v1`: ordered JSON array of `SavedLocation`.
- `recents_json_v1`: newest-first JSON array of `SavedLocation`.
- `onboarding_seen_v1`: Boolean; informational only, never proof of mock-app selection.

Rules:

- Kotlin Serialization defines an explicit version-1 disk DTO separate from domain model.
- Corrupt JSON returns empty data plus local actionable error/log; do not crash startup. Keep corruption handling test-covered.
- Favorites are unique by stable ID; optional labels trim to 60 Unicode code points; duplicate coordinates are allowed because labels may differ.
- Recents deduplicate by normalized latitude/longitude rounded to six decimal places, update timestamp, move item to front, and cap at 20.
- Write all repository mutations inside `DataStore.edit`; never maintain a second writable cache.
- Store only coordinates, labels, and timestamps locally. No device identity or actual-location history.
- No backup by default: set app data extraction/backup rules to exclude preferences containing selected/favorite locations, unless product owner explicitly chooses cloud device backup after privacy review.

## 9. Compose UI and accessibility

Single-screen responsive layout:

- Top app bar: Waypoint Shift and setup/help action.
- Main content: `GoogleMap`, selected draggable marker, and map tap selection.
- Coordinate card: latitude/longitude text fields, inline errors, Apply button, favorite toggle/save-label action.
- Bottom controls: prominent Start/Stop button and unambiguous “Mock location active/inactive” status.
- Favorites/Recents tabs or modal sheet: local entries, select, favorite rename/delete, clear recents.
- Setup sheet: numbered Developer Options instructions, Open Developer Options button using `Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS`, Return and Retry. Explain user must choose Waypoint Shift as mock location app.

UI state rules:

- Map tap or marker drag updates selected point and normalized manual fields.
- Manual Apply only succeeds when both finite and in range; invalid input never moves marker or starts service.
- While active, changing selection sends service Update explicitly and refreshes notification; show confirmation semantics in UI. No silent route/movement simulation.
- Start disabled until valid point exists and setup copy has been shown. Actual Play Services call decides whether setup is valid.
- Stop remains available in app and notification even during update/error states.
- Process recreation restores selection/favorites/recents from ViewModel/DataStore but does not automatically start mock mode.

Accessibility and quality:

- Material 3 dynamic color where available, light/dark themes, edge-to-edge insets.
- All icon buttons have content descriptions; map has surrounding instructions and non-map manual-coordinate alternative.
- Minimum 48 dp targets, logical focus order, screen-reader status announcements for start/stop/error, no color-only state.
- Coordinate fields use signed decimal keyboard while accepting pasted standard decimal text; locale behavior is explicitly tested. Persist and display canonical dot-decimal coordinates to avoid ambiguity.
- Test compact phone, large font, landscape, and API 36 predictive-back behavior for sheets/dialogs.

## 10. Planned paths

Root/build:

- `.gitignore`
- `README.md`
- `CHANGELOG.md`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradlew`
- `gradlew.bat`
- `local.properties.example`
- `local.defaults.properties`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`

Production Kotlin:

- `app/src/main/java/com/waypointshift/WaypointShiftApplication.kt`
- `app/src/main/java/com/waypointshift/AppContainer.kt`
- `app/src/main/java/com/waypointshift/MainActivity.kt`
- `app/src/main/java/com/waypointshift/model/GeoPoint.kt`
- `app/src/main/java/com/waypointshift/model/SavedLocation.kt`
- `app/src/main/java/com/waypointshift/data/LocationRepository.kt`
- `app/src/main/java/com/waypointshift/data/LocationPreferencesDataStore.kt`
- `app/src/main/java/com/waypointshift/data/LocationDiskDto.kt`
- `app/src/main/java/com/waypointshift/mock/MockLocationClient.kt`
- `app/src/main/java/com/waypointshift/mock/GoogleMockLocationClient.kt`
- `app/src/main/java/com/waypointshift/mock/MockLocationPublisher.kt`
- `app/src/main/java/com/waypointshift/service/MockLocationService.kt`
- `app/src/main/java/com/waypointshift/service/MockServiceCommand.kt`
- `app/src/main/java/com/waypointshift/service/MockServiceController.kt`
- `app/src/main/java/com/waypointshift/service/MockServiceStatus.kt`
- `app/src/main/java/com/waypointshift/notifications/MockLocationNotificationFactory.kt`
- `app/src/main/java/com/waypointshift/ui/MainUiState.kt`
- `app/src/main/java/com/waypointshift/ui/MainViewModel.kt`
- `app/src/main/java/com/waypointshift/ui/WaypointShiftApp.kt`
- `app/src/main/java/com/waypointshift/ui/MapScreen.kt`
- `app/src/main/java/com/waypointshift/ui/CoordinateControls.kt`
- `app/src/main/java/com/waypointshift/ui/SavedLocationsSheet.kt`
- `app/src/main/java/com/waypointshift/ui/DeveloperOptionsSheet.kt`
- `app/src/main/java/com/waypointshift/ui/theme/Color.kt`
- `app/src/main/java/com/waypointshift/ui/theme/Theme.kt`
- `app/src/main/java/com/waypointshift/ui/theme/Type.kt`

Tests:

- `app/src/test/java/com/waypointshift/model/GeoPointTest.kt`
- `app/src/test/java/com/waypointshift/data/LocationRepositoryTest.kt`
- `app/src/test/java/com/waypointshift/data/LocationDiskDtoTest.kt`
- `app/src/test/java/com/waypointshift/mock/MockLocationPublisherTest.kt`
- `app/src/test/java/com/waypointshift/service/MockServiceCommandTest.kt`
- `app/src/test/java/com/waypointshift/ui/MainViewModelTest.kt`
- `app/src/androidTest/java/com/waypointshift/data/LocationPreferencesDataStoreTest.kt`
- `app/src/androidTest/java/com/waypointshift/service/MockLocationServiceTest.kt`
- `app/src/androidTest/java/com/waypointshift/mock/GoogleMockLocationClientTest.kt`
- `app/src/androidTest/java/com/waypointshift/ui/WaypointShiftAppTest.kt`

## 11. Phased 2–5-minute TDD implementation tasks

Every numbered task is intended as one focused 2–5-minute action. For behavior changes, obey Red → Green → Refactor: write one failing assertion, run narrow test and observe expected failure, add minimum production code, rerun, then clean names/duplication while green. Do not batch all tests after implementation.

### Phase 0 — Bootstrap and prove API 36 toolchain

1. **[3 min]** Create `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, and `gradle/libs.versions.toml`; pin only verified stable plugin/tool versions.
2. **[3 min]** Generate Gradle wrapper paths under `gradle/wrapper/`, `gradlew`, and `gradlew.bat`; run `./gradlew --version` and verify JDK 17.
3. **[4 min]** Create `app/build.gradle.kts` with `compileSdk = 36`, `targetSdk = 36`, `minSdk = 23`, Kotlin/JVM 17, Compose, unit-test, instrumentation-test, and release minification configuration.
4. **[3 min]** Add current stable Compose BOM, Maps Compose, Play Services Location, Coroutines, DataStore Preferences, Kotlin Serialization, lifecycle/ViewModel, Activity Compose, and test dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
5. **[2 min]** Create `.gitignore`, `local.properties.example`, and `local.defaults.properties`; ensure `local.properties`, signing material, build output, IDE state, and secret property files stay ignored.
6. **[4 min]** Create minimal `app/src/main/AndroidManifest.xml`, `WaypointShiftApplication.kt`, `MainActivity.kt`, `strings.xml`, and Material theme files; launch empty Compose text.
7. **[3 min]** Create backup/data-extraction XML with local coordinate data excluded; wire manifest attributes.
8. **[3 min]** Add release key-placeholder validation in `app/build.gradle.kts`; verify debug unit configuration works without real key and release task fails clearly when placeholder remains.
9. **[5 min]** Run `./gradlew :app:assembleDebug :app:lintDebug`; fix only bootstrap failures.
10. **[3 min]** Install and launch debug APK on API 36 emulator; record `adb shell getprop ro.build.version.sdk` and verify value `36`.

Commit:

```bash
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties gradle gradlew gradlew.bat local.properties.example local.defaults.properties app
git commit -m "build: bootstrap API 36 Android app"
```

### Phase 1 — Coordinate domain and input contract

1. **[2 min, Red]** In `GeoPointTest.kt`, assert latitude below `-90` and above `90` is rejected; run `./gradlew :app:testDebugUnitTest --tests '*GeoPointTest'` and observe failure.
2. **[3 min, Green]** Add minimum range validation in `GeoPoint.kt`; rerun focused test.
3. **[2 min, Red]** Add longitude boundary/rejection cases for `-180`, `180`, and out-of-range values.
4. **[3 min, Green]** Add longitude validation; rerun focused test.
5. **[2 min, Red]** Add `NaN`, positive infinity, and negative infinity rejection tests.
6. **[2 min, Green]** Require finite doubles; rerun.
7. **[3 min, Red]** Add canonical six-decimal display and signed dot-decimal parsing tests, including whitespace and malformed input.
8. **[4 min, Green]** Implement parser/formatter in `GeoPoint.kt` without locale-dependent persisted format; rerun.
9. **[2 min, Refactor]** Convert boundary cases to parameterized tests and keep public error types/messages stable.

Commit:

```bash
git add app/src/main/java/com/waypointshift/model/GeoPoint.kt app/src/test/java/com/waypointshift/model/GeoPointTest.kt
git commit -m "feat: validate selected coordinates"
```

### Phase 2 — Local favorites and recents through Preferences DataStore

1. **[3 min, Red]** Add DTO round-trip test in `LocationDiskDtoTest.kt` for ID, label, coordinates, and timestamp.
2. **[4 min, Green]** Create `SavedLocation.kt` and versioned serializer DTO in `LocationDiskDto.kt`; rerun DTO test.
3. **[3 min, Red]** Add malformed/unknown-field JSON tests and expected safe result.
4. **[4 min, Green]** Implement tolerant decoder with explicit v1 mapping; rerun.
5. **[3 min, Red]** In `LocationRepositoryTest.kt`, define fake storage and assert add/rename/delete favorite behavior.
6. **[4 min, Green]** Add `LocationRepository.kt` contract and implementation against a small preferences storage interface; rerun.
7. **[3 min, Red]** Assert favorites allow same coordinate under distinct stable IDs and trim labels to 60 code points.
8. **[4 min, Green]** Add label normalization; rerun.
9. **[3 min, Red]** Assert recents normalize to six decimals, deduplicate, move newest to front, and cap at 20.
10. **[4 min, Green]** Implement recent insertion rules; rerun.
11. **[3 min, Red]** In `LocationPreferencesDataStoreTest.kt`, assert Preferences DataStore persists favorites/recents across recreated repository instances.
12. **[5 min, Green]** Add singleton `LocationPreferencesDataStore.kt` and Preferences keys; run connected focused test.
13. **[3 min, Red]** Add instrumentation case for corrupt JSON recovering to empty list without crash.
14. **[4 min, Green]** Add corruption logging/error mapping; rerun.
15. **[2 min, Refactor]** Remove duplicate normalization paths and ensure immutable emitted lists.

Commit:

```bash
git add app/src/main/java/com/waypointshift/model/SavedLocation.kt app/src/main/java/com/waypointshift/data app/src/test/java/com/waypointshift/data app/src/androidTest/java/com/waypointshift/data
git commit -m "feat: persist local favorites and recents"
```

### Phase 3 — Mock publisher core, independent from Android service

1. **[3 min, Red]** In `MockLocationPublisherTest.kt`, use fake `MockLocationClient` and test `start(point)` calls `enable()` before first `publish()`.
2. **[4 min, Green]** Add `MockLocationClient.kt` and minimum `MockLocationPublisher.kt`; rerun.
3. **[3 min, Red]** With coroutine test scheduler, assert first publish is immediate and later publishes occur every one second without overlap.
4. **[5 min, Green]** Add one owned publication job and monotonic delay loop; rerun.
5. **[3 min, Red]** Assert second Start changes point but does not call `enable()` twice or create two loops.
6. **[4 min, Green]** Serialize update and make start idempotent; rerun.
7. **[3 min, Red]** Assert Stop cancels publication before `disable()` and repeated Stop is safe.
8. **[4 min, Green]** Add ordered, idempotent stop; rerun.
9. **[3 min, Red]** Assert publish failure triggers `disable()`, failed status, and no later publishes.
10. **[5 min, Green]** Add failure cleanup under `NonCancellable` with bounded timeout; rerun.
11. **[3 min, Red]** Assert cancellation during enable/first publish still attempts disable once.
12. **[4 min, Green]** Close cancellation gap and rerun full publisher test.
13. **[2 min, Refactor]** Extract injected wall/elapsed clocks and interval; production default stays one second.

Commit:

```bash
git add app/src/main/java/com/waypointshift/mock/MockLocationClient.kt app/src/main/java/com/waypointshift/mock/MockLocationPublisher.kt app/src/test/java/com/waypointshift/mock/MockLocationPublisherTest.kt
git commit -m "feat: add safe mock publication loop"
```

### Phase 4 — Google Play Services mock client

1. **[3 min, Red]** Add a unit-test seam around location construction and assert provider, coordinate, accuracy, wall time, and elapsed realtime.
2. **[4 min, Green]** Create `GoogleMockLocationClient.kt` and valid `Location` construction; rerun unit test.
3. **[3 min, Red]** Assert `enable`, `publish`, and `disable` map exactly to `setMockMode(true)`, `setMockLocation(location)`, and `setMockMode(false)` in order.
4. **[4 min, Green]** Add await-based Fused client adapter; no retries yet; rerun.
5. **[3 min, Red]** Assert security/API failures retain cause and map to typed domain failures.
6. **[4 min, Green]** Add minimal failure mapping; rerun.
7. **[3 min]** Add manifest mock-location permission and verify merged debug manifest contains it without real-location permissions.
8. **[4 min, Red]** In `GoogleMockLocationClientTest.kt`, add API 36 instrumentation assertion that published location is reported as mock and has expected coordinate.
9. **[5 min, Green]** Configure API 36 emulator test app as selected mock app, run focused connected test, then ensure test teardown calls `setMockMode(false)` in `finally`.
10. **[3 min]** Add negative instrumentation run without mock app selection and verify actionable permission failure; restore test app selection afterward.

Developer-machine test setup only:

```bash
adb shell appops set com.waypointshift android:mock_location allow
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.waypointshift.mock.GoogleMockLocationClientTest
adb shell appops set com.waypointshift android:mock_location deny
```

The app itself must never execute these commands.

Commit:

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/waypointshift/mock/GoogleMockLocationClient.kt app/src/test/java/com/waypointshift/mock app/src/androidTest/java/com/waypointshift/mock
git commit -m "feat: publish transparent fused mock locations"
```

### Phase 5 — Foreground service, notification, and lifecycle cleanup

1. **[3 min, Red]** In `MockServiceCommandTest.kt`, assert only finite validated coordinates decode from Start/Update intents and malformed intents become safe no-ops/errors.
2. **[4 min, Green]** Add `MockServiceCommand.kt`; rerun.
3. **[3 min, Red]** Add notification-factory tests for active title, exact coordinate content, ongoing flag, and Stop pending intent.
4. **[5 min, Green]** Create `MockLocationNotificationFactory.kt`, immutable/update-current pending intents, and low-importance channel.
5. **[3 min]** Add exact FGS permissions, `specialUse` service type, subtype property, non-exported service, and notification strings to manifest/resources.
6. **[3 min, Red]** In `MockLocationServiceTest.kt`, inject fake app container and assert Start calls `startForeground` before publisher setup.
7. **[5 min, Green]** Add `AppContainer.kt`, `WaypointShiftApplication.kt`, and minimum `MockLocationService.kt`; run focused connected test.
8. **[3 min, Red]** Assert Update changes publisher point and notification without another foreground service instance.
9. **[4 min, Green]** Add serialized Update handling; rerun.
10. **[3 min, Red]** Assert notification Stop calls publisher stop, removes notification, and stops service.
11. **[5 min, Green]** Add Stop action handling and rerun.
12. **[3 min, Red]** Assert null intent, malformed command, `onDestroy`, and startup failure all attempt idempotent cleanup.
13. **[5 min, Green]** Add unified cleanup path and `START_NOT_STICKY`; rerun.
14. **[3 min, Red]** Assert no boot/receiver component exists and service is not exported using merged-manifest test or script assertion.
15. **[3 min, Green]** Fix manifest and rerun manifest checks.
16. **[4 min]** Manually force-stop/kill service on API 36; document observed behavior and verify next launch best-effort reconciliation does not auto-start.

Commit:

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/com/waypointshift/AppContainer.kt app/src/main/java/com/waypointshift/WaypointShiftApplication.kt app/src/main/java/com/waypointshift/service app/src/main/java/com/waypointshift/notifications app/src/test/java/com/waypointshift/service app/src/androidTest/java/com/waypointshift/service
git commit -m "feat: run mocking in user-visible foreground service"
```

### Phase 6 — ViewModel state and service controller

1. **[3 min, Red]** In `MainViewModelTest.kt`, assert initial repository snapshot produces inactive UI with no auto-start.
2. **[4 min, Green]** Add `MainUiState.kt`, `MainViewModel.kt`, and fakeable dependencies; rerun.
3. **[3 min, Red]** Assert map selection updates canonical fields and valid Start availability.
4. **[4 min, Green]** Add selection reducer; rerun.
5. **[3 min, Red]** Assert invalid manual input emits field errors and does not change selection.
6. **[4 min, Green]** Add manual-input state/actions; rerun.
7. **[3 min, Red]** Assert Start click records recent only after service accepts command and never from state restoration.
8. **[5 min, Green]** Add `MockServiceController.kt`, explicit service intent, and recent insertion ordering; rerun.
9. **[3 min, Red]** Assert active coordinate change emits one Update and Stop remains enabled.
10. **[4 min, Green]** Add active update and stop actions; rerun.
11. **[3 min, Red]** Assert mock-app permission failure exposes setup action while preserving valid selection.
12. **[4 min, Green]** Map service status/error into UI state; rerun.
13. **[3 min, Red]** Assert favorite save/rename/delete and recent clear call repository exactly once.
14. **[4 min, Green]** Add persistence actions; rerun.
15. **[2 min, Refactor]** Consolidate state transformations and rerun entire ViewModel suite.

Commit:

```bash
git add app/src/main/java/com/waypointshift/service/MockServiceController.kt app/src/main/java/com/waypointshift/service/MockServiceStatus.kt app/src/main/java/com/waypointshift/ui/MainUiState.kt app/src/main/java/com/waypointshift/ui/MainViewModel.kt app/src/test/java/com/waypointshift/ui/MainViewModelTest.kt
git commit -m "feat: coordinate mock session UI state"
```

### Phase 7 — Compose controls and Developer Options onboarding

1. **[3 min, Red]** In `WaypointShiftAppTest.kt`, assert inactive state shows selected coordinate fields, Start, Favorites, Recents, and help semantics.
2. **[5 min, Green]** Create `WaypointShiftApp.kt` and `CoordinateControls.kt`; rerun connected UI test.
3. **[3 min, Red]** Assert invalid field state displays accessible inline messages and disables Apply/Start.
4. **[4 min, Green]** Render validation semantics and keyboard options; rerun.
5. **[3 min, Red]** Assert active state announces “Mock location active”, replaces Start with Stop, and keeps coordinate controls usable.
6. **[4 min, Green]** Render active status/actions; rerun.
7. **[3 min, Red]** Assert setup sheet contains numbered instructions, transparency warning, Open Developer Options, Retry, and dismiss/return path.
8. **[5 min, Green]** Create `DeveloperOptionsSheet.kt` and public settings intent launcher; rerun.
9. **[3 min, Red]** Assert Android 13+ notification rationale appears before first Start and denial behavior is explicit.
10. **[5 min, Green]** Add notification permission launcher in `MainActivity.kt`; block Start per transparent product decision until granted or owner revises policy.
11. **[3 min]** Add screen-reader announcements and content descriptions; rerun UI semantics tests.
12. **[4 min]** Test font scale 2.0 and landscape on API 36; fix clipping without screenshot-only assertions.

Commit:

```bash
git add app/src/main/java/com/waypointshift/MainActivity.kt app/src/main/java/com/waypointshift/ui/WaypointShiftApp.kt app/src/main/java/com/waypointshift/ui/CoordinateControls.kt app/src/main/java/com/waypointshift/ui/DeveloperOptionsSheet.kt app/src/androidTest/java/com/waypointshift/ui/WaypointShiftAppTest.kt app/src/main/res/values/strings.xml
git commit -m "feat: add accessible setup and session controls"
```

### Phase 8 — Maps Compose selection and saved-location UI

1. **[3 min, Red]** Extend ViewModel test: map tap selects point and updates fields; marker drag uses same event path.
2. **[4 min, Green]** Create `MapScreen.kt` with `GoogleMap`, selected `Marker`, tap callback, and draggable marker callback; rerun ViewModel tests.
3. **[3 min, Red]** Add UI semantics assertion for manual coordinate fallback/instructions when map is unavailable.
4. **[4 min, Green]** Add map loading/error overlay and retain controls independent of map render.
5. **[3 min, Red]** Assert favorite and recent rows select coordinates and expose only valid row actions.
6. **[5 min, Green]** Create `SavedLocationsSheet.kt`; rerun UI test.
7. **[3 min, Red]** Assert favorite label entry trims/validates and delete requires clear confirmation.
8. **[4 min, Green]** Add label dialog and delete confirmation; rerun.
9. **[3 min, Red]** Assert active map selection sends explicit Update once after drag end, not on every drag frame.
10. **[5 min, Green]** Separate marker preview from committed drag-end update; rerun.
11. **[4 min]** Manual API 36 map test: tap, drag, zoom, rotate, apply manual coordinate, select saved item, light/dark map surroundings, offline tile failure.
12. **[3 min]** Verify restricted debug Maps key renders tiles and key does not appear in tracked files or logs.

Commit:

```bash
git add app/src/main/java/com/waypointshift/ui/MapScreen.kt app/src/main/java/com/waypointshift/ui/SavedLocationsSheet.kt app/src/main/java/com/waypointshift/ui/WaypointShiftApp.kt app/src/main/java/com/waypointshift/ui/MainViewModel.kt app/src/test/java/com/waypointshift/ui app/src/androidTest/java/com/waypointshift/ui app/src/main/res/values/strings.xml
git commit -m "feat: select and save locations from map"
```

### Phase 9 — Full lifecycle, failure, and privacy verification

1. **[3 min]** Start on API 36 from visible Activity; verify notification appears immediately and `adb shell dumpsys activity services com.waypointshift` reports `specialUse` FGS.
2. **[3 min]** Verify another test app/location probe receives chosen coordinate with mock flag visible; do not test or implement concealment.
3. **[3 min]** Change coordinate by map and manual input while active; verify next publications and notification update.
4. **[3 min]** Stop from app; verify `setMockMode(false)` behavior, service absence, and notification removal.
5. **[3 min]** Start again, stop from notification; verify same cleanup.
6. **[4 min]** Remove mock-app selection, press Start, verify safe stop and actionable Developer Options flow.
7. **[4 min]** Deny notifications on API 36 and verify product blocks Start with rationale; grant and retry.
8. **[4 min]** Background app after active user start; verify publication continues. Attempt no background start path because none may exist.
9. **[4 min]** Swipe task away; verify service behavior matches `stopWithTask=false` and notification Stop remains functional.
10. **[4 min]** Force-stop app; verify no auto-restart. Relaunch and verify reconciliation plus inactive UI.
11. **[3 min]** Reboot emulator; verify no receiver, no service, and no automatic mocking.
12. **[4 min]** Disable/outdate Play Services where practical; verify repair/error path and cleanup.
13. **[4 min]** Corrupt DataStore test fixture; verify app starts, reports local issue, and does not send data.
14. **[3 min]** Inspect merged manifests for debug/release; verify no real-location, background-location, boot, analytics, advertising ID, account, exported service, or unexpected permissions/components.
15. **[4 min]** Run APK Analyzer/dependency report; verify no analytics/backend SDK and review all transitive dependencies.
16. **[5 min]** Run API 23 smoke test for launch, manual coordinate, Start/Stop, and notification compatibility.
17. **[5 min]** Run API 36 large-font, landscape, dark-mode, TalkBack, and predictive-back smoke tests.

Commit only fixes arising from this phase, grouped by behavior and each preceded by narrow tests. Example:

```bash
git add <exact-fixed-paths>
git commit -m "fix: preserve mock cleanup across service exits"
```

### Phase 10 — Documentation, release hardening, and final gate

1. **[4 min]** Write `README.md`: product boundaries, setup, Developer Options flow, Maps key setup, build/run commands, and limitations.
2. **[3 min]** Write `CHANGELOG.md` entry for `0.1.0` user-visible behavior.
3. **[4 min]** Review `app/proguard-rules.pro` and release R8 output; add only rules proven necessary by release build/runtime.
4. **[4 min]** Verify release key check rejects absent/placeholder Maps key and restricted real key builds without printing value.
5. **[5 min]** Run clean unit, lint, instrumentation, and release build commands below.
6. **[3 min]** Inspect `git diff --check`, `git status --short`, and tracked files for accidental secrets.
7. **[4 min]** Perform signed release candidate smoke test on physical API 36 device selected as mock app.
8. **[3 min]** Record exact tested Android/Play Services versions and unresolved OEM/store-policy risks in release notes.

Commit:

```bash
git add README.md CHANGELOG.md app/proguard-rules.pro local.properties.example
git commit -m "docs: document setup and release safeguards"
```

No push without explicit owner instruction.

## 12. Verification commands

Fast red/green loop:

```bash
./gradlew :app:testDebugUnitTest --tests '*GeoPointTest'
./gradlew :app:testDebugUnitTest --tests '*LocationRepositoryTest'
./gradlew :app:testDebugUnitTest --tests '*MockLocationPublisherTest'
./gradlew :app:testDebugUnitTest --tests '*MainViewModelTest'
```

Local static/build gate:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

API 36 emulator gate:

```bash
adb shell getprop ro.build.version.sdk
adb shell appops set com.waypointshift android:mock_location allow
./gradlew :app:connectedDebugAndroidTest
adb shell dumpsys activity services com.waypointshift
adb shell appops set com.waypointshift android:mock_location deny
```

Release gate, with secret supplied locally and never printed:

```bash
./gradlew clean
./gradlew :app:testDebugUnitTest :app:lintRelease :app:connectedDebugAndroidTest :app:assembleRelease
./gradlew :app:dependencies
git diff --check
git status --short
git ls-files | xargs grep -nE 'MAPS_API_KEY=[^Y]|AIza[0-9A-Za-z_-]{20,}'
```

If secret scan command produces a false positive from placeholder/docs, inspect every match; never suppress a real credential. Avoid passing secret itself as command-line argument because shell history/process listings can expose it.

Useful manual inspection:

```bash
./gradlew :app:processDebugMainManifest
adb shell dumpsys package com.waypointshift
adb logcat --pid="$(adb shell pidof com.waypointshift)"
```

## 13. Release acceptance criteria

- API 36 compile/target and stable toolchain confirmed; unit, lint, connected API 36 tests, release build, and physical-device smoke test pass.
- User can select through map tap, marker drag, manual fields, favorites, and recents.
- Start occurs only from explicit visible user action with valid coordinate and notification permission policy satisfied.
- `FusedLocationProviderClient.setMockMode(true)` precedes publication; every normal/error stop path attempts `setMockMode(false)`.
- Published location has current wall/elapsed timestamps, selected coordinate, expected accuracy, and remains visibly mock.
- One publication loop and one service instance exist despite repeated Start/Update commands.
- Ongoing notification states active mocking and offers working Stop.
- No background auto-start, boot start, sticky restart, hidden API, concealment, real/background location permission, or settings mutation exists.
- Favorites/recents survive process recreation, remain local, obey deduplication/cap rules, and are excluded from backup per privacy default.
- Restricted Maps key is absent from Git and logs; placeholder release build fails clearly.
- App handles not-selected-as-mock-app, missing/outdated Play Services, map-key/tile failure, malformed coordinate, corrupt local data, notification denial, process death, and reboot without silent failure or automatic mocking.
- Merged manifest and dependency review confirm no ads, accounts, analytics, telemetry, backend, advertising ID, or unexpected exported components.

## 14. Official references to re-check before implementation/release

- Android 16/API 36 SDK setup: <https://developer.android.com/about/versions/16/setup-sdk>
- Android foreground-service launch restrictions: <https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start>
- Android foreground-service types: <https://developer.android.com/develop/background-work/services/fgs/service-types>
- `FusedLocationProviderClient`: <https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient>
- Maps Compose: <https://developers.google.com/maps/documentation/android-sdk/maps-compose>
- Maps Secrets Gradle Plugin: <https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin>
- Preferences DataStore: <https://developer.android.com/topic/libraries/architecture/datastore>
- Compose testing: <https://developer.android.com/develop/ui/compose/testing>

Platform and store rules change. Treat these current official pages—not copied snippets in this plan—as authority when implementation begins.
