# Contributing — MBHM

Thanks for contributing to MBHM! This document covers how to set up, make changes, and submit contributions.

---

## 1. Setup

1. Fork and clone the repository.
2. Open the `MBHM` folder in Android Studio.
3. Allow Gradle sync and install the Android SDK for compileSdk 37.
4. Build: `./gradlew :app:assembleDebug`
5. Run tests: `./gradlew test`

To run the sync server for local testing, see [GETTING_STARTED.md](GETTING_STARTED.md) section 3.

---

## 2. Code style

- **Language**: Kotlin. UI is built with **Jetpack Compose** and **Material 3**.
- **Theme**: Follow the existing blue-and-white brand palette in `ui/theme/Color.kt` and typography in `ui/theme/Type.kt`. Reuse shared components in `ui/components/` where possible.
- **Architecture**: UI → `AppRepository` → Room (entities + DAOs). Domain models live in `model/` and are independent of Android/Room. Sync logic lives in `data/sync/`.
- **Naming**: Files and classes use PascalCase; functions and variables use camelCase. Follow the existing package structure (`guardian/`, `boarder/`, `data/`, `ui/`).
- **Formatting**: Use the project's Kotlin style (4-space indentation, no trailing whitespace).

---

## 3. Adding a new feature

A typical feature (e.g., a new managed entity) involves:

1. **Domain model**: Add a data class in `model/Models.kt` (or a new file in `model/`).
2. **Room entity**: Add an `@Entity` data class in `data/entity/` with `toDomain()`/`fromDomain()` converters.
3. **DAO**: Add query methods in the appropriate DAO under `data/dao/`.
4. **Database**: Register the entity in `AppDatabase.kt`; bump the version and add a migration if needed.
5. **Repository**: Add observation and write methods to `AppRepository.kt`.
6. **UI**: Add Compose screens under `guardian/` and/or `boarder/`, and register them in the role shell's bottom navigation.
7. **Sync**: The entity is automatically included in sync if added to the server's `COLLECTIONS` in `server/server.js` and mirrored in the client's push/pull payloads.
8. **Tests**: Add unit tests under `app/src/test/` and/or instrumented tests under `app/src/androidTest/`.

---

## 4. Testing

```bash
# Unit tests (no device required)
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

Test packages:
- `app/src/test/java/com/example/mbhm/` — unit tests (e.g. `PaymentVerificationTest`, `ExampleUnitTest`)
- `app/src/androidTest/java/com/example/mbhm/` — instrumented tests (e.g. `ExampleInstrumentedTest`)

---

## 5. Commit and pull request process

1. Create a feature branch from `main`.
2. Make your changes, keeping commits focused and atomic.
3. Write or update tests as appropriate.
4. Update documentation in `docs/` if your change affects setup, architecture, sync, API, database, security, or the user guide.
5. Open a pull request with a clear description of the change and how to test it.
6. Ensure the project builds (`./gradlew build`) and tests pass before requesting review.

---

## 6. Documentation

When contributing, keep these docs in sync with code changes:

| Doc | Covers |
|-----|--------|
| `README.md` | Project overview, quick start, server setup, security limitations |
| `docs/GETTING_STARTED.md` | Developer onboarding |
| `docs/ARCHITECTURE.md` | App layers, data flow, sync server |
| `docs/SYNC.md` | Wireless and phone-to-phone sync |
| `docs/API.md` | Sync server endpoints |
| `docs/DATABASE.md` | Room schema reference |
| `docs/SECURITY.md` | Threat model, limitations, hardening |
| `docs/USER_GUIDE.md` | Feature walkthrough per role |
| `docs/CHANGELOG.md` | Versioned change history |
| `docs/CONTRIBUTING.md` | This file |

---

## 7. Questions

Open an issue or discussion in the repository for questions about architecture, sync behavior, or security limitations.
