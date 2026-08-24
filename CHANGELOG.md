# Changelog

## Unreleased

### Added

- API 36 Android application foundation using Kotlin, Jetpack Compose, and Material 3.
- Gradle 9.5.0 wrapper with verified distribution checksum.
- Validated coordinate value object with canonical six-decimal formatting.
- Coordinate unit tests and local-data backup exclusions.
- Manual coordinate parsing accepts valid trailing decimal input such as `41.` while retaining range and finite-value checks.
- Local favorites and recents domain repository backed by Preferences DataStore and Kotlin Serialization.
- Tolerant JSON decoding ignoring unknown future fields and recovering gracefully from corrupt preference data.
- Stable-ID favorite management with label trimming capped at 60 Unicode code points.
- Normalized 6-decimal recents deduplication, newest-first ordering, and 20-item cap.

Map UI and mock-location service remain intentionally outside this foundation phase.
