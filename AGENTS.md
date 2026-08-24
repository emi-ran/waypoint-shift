# Waypoint Shift Development Rules

## Product boundaries

- Transparent, no-root Android mock-location tool using official Android APIs only.
- Never hide mock state, bypass `Location.isMock()`, evade detection, alter developer settings, use root/hooking, add tracking, ads, accounts, telemetry, or backend services.
- Mocking starts only after explicit user action and stops reliably through every cleanup path.
- Use least privilege. Do not add background location permission unless proven necessary.

## Engineering

- Kotlin, Jetpack Compose, Material 3, Coroutines, StateFlow, ViewModel, DataStore Preferences.
- Use official current Android APIs. Do not use hidden APIs.
- Keep secrets out of Git. `local.properties` is ignored; document values in `local.properties.example`.
- Add/update tests before production behavior changes where practical.
- Run narrow checks before each commit. Commit small coherent verified slices.
- Update user-facing docs and changelog with user-visible work.

## Delivery

- No push without explicit user instruction.
- Report only checks actually run and known blockers.
