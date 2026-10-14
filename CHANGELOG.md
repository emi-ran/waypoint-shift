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
- Hot repository `StateFlow` snapshots for favorites, recents, and onboarding state.
- Transparent Google Play services mock-location client and safe one-second publication loop.
- User-visible `specialUse` foreground service with exact-coordinate notification and Stop action.
- ViewModel-driven map tap, marker drag-end, and validated manual coordinate selection.
- Developer Options onboarding plus Android 13+ notification permission gate before Start.
- Local favorite creation, selection, rename, confirmed delete, and recent selection/clear UI.
- Comprehensive English and Turkish documentation (`README.md` and `README.tr.md`) with setup guides and architecture overviews.

### Changed

- Removed obsolete Google Maps API key placeholders and cleaned up local properties configuration.
- Favorite updates and repeated recent selections now preserve original creation time while recording latest use time.
- Application DataStore access now uses one process-wide delegated instance.
- Repository mutations return only after public `StateFlow` snapshots reflect committed storage state.
- Initial setup now checks Android's current mock-location AppOp where permitted, avoids repeating selection instructions after successful Return and retry, and reports separate actionable diagnostics for denied AppOp, unavailable Google Play services, Fused provider failures, and foreground-service start failures.

### Privacy

- No real/background location permission, tracking, analytics, telemetry, accounts, backend, or cloud sync.
- Favorites and recents remain local and excluded from Android backup.
- Mock state remains visible to Android and other apps; app never changes Developer Options or conceals mock status.
