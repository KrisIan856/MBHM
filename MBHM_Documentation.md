# MBHM — Boarding House Management Hub
## Complete Project Documentation

---

**Document Control**

| Field | Value |
|-------|-------|
| Project Name | MBHM — Boarding House Management Hub |
| Platform | Android (Kotlin + Jetpack Compose + Room) |
| Companion Server | Node.js HTTP sync server |
| Documentation Version | 1.0 |
| Status | Student / Demo Build |

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Features](#2-features)
3. [Getting Started](#3-getting-started)
4. [Architecture](#4-architecture)
5. [User Guide — Guardian Role](#5-user-guide-guardian-role)
6. [User Guide — Boarder Role](#6-user-guide-boarder-role)
7. [Shared Screens](#7-shared-screens)
8. [Synchronization](#8-synchronization)
9. [Sync Server API Reference](#9-sync-server-api-reference)
10. [Database Schema](#10-database-schema)
11. [Security](#11-security)
12. [Contributing](#12-contributing)
13. [Changelog](#13-changelog)

---

## 1. Project Overview

MBHM (Boarding House Management Hub) is an Android application for managing a boarding house. It provides two distinct user interfaces:

* **Guardian** — manages boarders, payments, attendance, announcements, rooms, leave notices, incidents, and maintenance reports.
* **Boarder** — views announcements, submits payments with receipts, checks in to attendance sessions, and submits leave notices.

The app uses Jetpack Compose for the UI, Room (SQLite) for local data persistence, and a companion Node.js sync server running on a computer on the same local network for device synchronization. Direct phone-to-phone sync over Wi-Fi is also supported.

**Note:** This is a student/demo build, not a production deployment. Review the security limitations in Section 11 before using MBHM with real sensitive records.

---

## 2. Features

| Category | Features |
|----------|----------|
| Authentication | Sign-in / registration with role selection (Guardian / Boarder), "Remember me" session persistence, 10-minute inactivity timeout |
| Dashboard (Guardian) | Time-based greeting, stat cards (Fully Paid, Partially Paid, Present Today, Action Needed), monthly collection progress bar, grouped quick actions |
| Payments | Payment records by period, billing settings (due day, grace period, penalties, reminders, channels), receipt verification / rejection by guardian |
| Attendance | Morning/evening check-in sessions from worship schedule, QR or manual check-in, enforced attendance window and QR validity deadline, absence alerts |
| Security | Boarder account creation, ID numbers and passwords, incident logging (SOS, missed curfew, missed worship, maintenance, other), maintenance reports, room and job management, emergency contacts |
| Leave Notices | Boarder submission with reason, destination, dates; Guardian approve / reject / mark departed / mark arrived workflow |
| Announcements | Guardian-posted announcements with priority (Normal / Critical) and optional recipient targeting |
| Maintenance | Maintenance ticket submission and resolution tracking (Plumbing, Electrical, Furniture, Appliance, Other) |
| Sync | Wireless sync to computer server, direct phone-to-phone sync with pairing token, automatic sync every 5 seconds |
| Server Dashboard | Browser-based shared database view at `http://<computer-ip>:3000` |

---

## 3. Getting Started

### 3.1 Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | Latest stable | Used to open and build the project |
| JDK | 17 | Required by Gradle / Android Gradle Plugin |
| Android SDK | compileSdk 37 | Specified in `app/build.gradle.kts` |
| Node.js | 22.5+ | Only needed to run the computer sync server |
| Git | Any | For cloning the repository |

### 3.2 Clone and Open the Project

```bash
git clone <repository-url>
cd MBHM
```

Open the `MBHM` folder in Android Studio. Allow Gradle sync to complete. The project uses Kotlin + Jetpack Compose with Room for local storage.

> The source archive excludes `local.properties`, Gradle caches, and generated build output. Android Studio will regenerate these on first sync/build.

### 3.3 Build and Run the Android App

**From Android Studio:**
1. Select the `app` run configuration.
2. Choose a connected device or emulator (API level matching compileSdk 37 recommended).
3. Click **Run**.

**From the command line:**
```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug
```

### 3.4 Run the Computer Sync Server (Windows)

The sync server is a small Node.js HTTP server in the `server/` folder. It exposes a local dashboard and sync endpoints (`/api/status`, `/api/export`, `/api/pull`, `/api/push`) that the Android app connects to over the LAN.

1. Install **Node.js 22.5 or newer**.
2. Open the `server` folder and double-click `start-server.bat` (or run `node server.js`).
3. On first launch, the script creates `server/.sync-token`, prints the access token, and starts the server on port 3000. Keep the token private — it persists across restarts and is excluded from Git.
4. Find the computer's IPv4 address in the server window.
5. Open a browser at `http://<computer-ip>:3000` for the shared-database dashboard. Login: username `mbhm`, password = access token.

If the token is lost, stop the server, delete `server/.sync-token`, and restart `start-server.bat` to generate a new one. Enter the new token on each phone.

### 3.5 Connect the App to the Server

1. In MBHM, open **Wireless Sync**.
2. Under **Server Address**, enter the computer's IPv4 address (e.g. `http://192.168.1.42:3000`).
3. Under **Server Access Token**, paste the access token printed by the server.
4. Tap **Save Address** or **Sync Now**.

The app stores the token encrypted using Android Keystore and sends it as a Bearer token on each request.

### 3.6 Direct Phone-to-Phone Sync

1. On the guardian phone, open **Wireless Sync** and tap **Start Sharing**. Boarder devices can join a host but cannot host.
2. Share the host phone's IP address and the displayed pairing token with the intended device only.
3. On the other phone, enter the host IP and token, then tap **Sync With Device**.
4. Tap **Stop Sharing** when finished. The pairing token is invalidated when sharing stops.

### 3.7 Run Tests

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (requires a connected device/emulator)
./gradlew connectedAndroidTest
```

Test sources live under `app/src/test/` (unit) and `app/src/androidTest/` (instrumented).

---

## 4. Architecture

MBHM follows a clean separation between UI, domain, and data layers, with a single `AppRepository` acting as the source of truth.

### 4.1 High-Level Data Flow

```
UI (Compose screens)
        ↓ (collect state / call methods)
AppRepository  ← single instance, exposes Flows and suspend functions
        ↓ (Room DAO calls)
Room database (entities + DAOs)
        ↓ (sync)
SyncManager → Sync client → Remote sync server (Node.js)  [wireless]
            → Local sync client/server                    [phone-to-phone]
```

### 4.2 UI Layer

* **Theme** (`ui/theme/`): Material 3 color (`Color.kt`), typography (`Type.kt`), and component theme (`Theme.kt`). The project uses a consistent blue-and-white brand palette.
* **Components** (`ui/components/`): Shared Compose building blocks — `StatCard`, `PCard`, `EmptyState`, `ChipBar`, `DateRangeFilter`, `NavBar`, `NavItem`, `Modal`, `Toast`, QR display, money formatting, etc.
* **Auth** (`ui/auth/AuthScreen.kt`): Sign-in / registration screen with role selection and credential entry panels. "Remember me" controls session persistence across cold starts.

**Role shells** (each with a five-item bottom navigation):

| Role | Shell File | Tabs |
|------|-----------|------|
| Guardian | `guardian/GuardianApp.kt` | Dashboard, Payments, Attendance, Security, More |
| Boarder | `boarder/BoarderApp.kt` | Boarder-facing equivalents |

The root `App.kt` (`MbhmApp`) routes between `AuthScreen`, `GuardianApp`, and `BoarderApp` based on the signed-in user's `Role` (`GUARDIAN` / `BOARDER`). It also seeds sample data on first launch (`seedIfEmpty()`) and maintains attendance sessions on a 15-second tick.

### 4.3 Domain Layer (`model/`)

Plain Kotlin data classes and enums independent of Android and Room:

* Enums: `Role`, `PayStatus`, `ReceiptStatus`, `IncidentType`, `CurfewStatus`, `LeaveNoticeStatus`, `PayMethod`, `RecordedBy`, `AttendanceStatus`, `AttendanceMethod`, `SessionType`, `Priority`, `PenaltyType`, `MaintCategory`, `MaintStatus`
* Models: `Boarder`, `PaymentRecord`, `AttendanceSession`, `AttendanceRecord`, `Announcement`, `VisitorEntry`, `Incident`, `MaintenanceReport`, `CurfewRecord`, `WorshipSchedule`, `AbsenceAlertRecord`, `BillingSettings`, `ReminderSettings`, `ChannelSettings`, `GuardianProfile`, `GuardianDetails`, `HouseRoom`, `HouseJob`, `EmergencyContact`, `GuardianContact`, `NewBoarderInput`

### 4.4 Data Layer (`data/`)

**Repository:** `data/repository/AppRepository.kt` is a singleton (`getInstance(context)`) that exposes all collections as Flow-based observation methods plus suspend upsert/update methods used by sync and UI actions. `seedIfEmpty()` populates demo data on first launch.

**Room Database:** `data/database/AppDatabase.kt` defines the database with entities and DAOs under `data/entity/` and `data/dao/`. Each entity has `toDomain()` / `fromDomain()` converters between the Room entity and the domain model. `data/converter/Converters.kt` holds Room type converters for enums and lists.

| Table | Entity File | DAO |
|-------|-------------|-----|
| users | `UserEntity` | `UserDao` |
| boarders | `BoarderEntity` | `HouseDaos` / `BoarderDao` |
| rooms | `RoomEntity` | `HouseDaos` |
| jobs | `JobEntity` | `HouseDaos` |
| guardian_profile | `GuardianProfileEntity` | — |
| payment_records | `PaymentRecordEntity` | — |
| attendance_sessions | `AttendanceSessionEntity` | — |
| attendance_records | `AttendanceRecordEntity` | — |
| announcements | `AnnouncementEntity` | — |
| visitor_entries | `VisitorEntryEntity` | — |
| incidents | `IncidentEntity` | — |
| maintenance_reports | `MaintenanceReportEntity` | — |
| curfew_records | `CurfewRecordEntity` | — |
| worship_schedule | `WorshipScheduleEntity` | — |
| billing_settings | `BillingSettingsEntity` | — |
| leave_notices | `LeaveNoticeEntity` | `LeaveNoticeDao` |
| absence_alerts | `AbsenceAlertEntity` | `AbsenceAlertDao` |

### 4.5 Sync Layer (`data/sync/`)

* **`SyncManager`** — singleton orchestrator. Holds host, auto-sync flag, bearer token, and client id in `SharedPreferences` (`mbhm_sync`). Runs auto-sync every 5 seconds when host, token, and network are available and not busy. Exposes a `StateFlow<SyncState>`.
* **`RemoteSyncClient`** — HTTP client that talks to the Node.js server (`push`, `pull`, `export`, `status`). Sends the bearer token stored in `SecureTokenStore`.
* **`SecureTokenStore`** — stores the server access token encrypted with Android Keystore.
* **`LocalSyncClient` / `LocalSyncServer`** — direct phone-to-phone sync over Wi-Fi using a pairing token.
* **`NetworkUtils`** — network reachability helpers.

### 4.6 Sync Server (`server/`)

A single-file Node.js HTTP server (`server.js`) using the built-in `node:sqlite` module:

* **Database:** `mbhm_server.db` (WAL journal mode), one `records` table versioned by an incrementing `version` column, plus `client_rows` for per-client known versions and a `meta` table.
* **Collections:** users, boarders, paymentRecords, attendanceSessions, attendanceRecords, announcements, visitorEntries, incidents, maintenanceReports, curfewRecords, worshipSchedules, billingSettings, leaveNotices, absenceAlerts, rooms, jobs, guardianProfile.
* **Endpoints:** `GET /` (dashboard), `GET /api/status`, `GET /api/export`, `GET /api/pull?since=`, `POST /api/push`.
* **Auth:** `Authorization: Bearer <token>` or `Basic mbhm:<token>`. Token loaded from `SYNC_TOKEN` env, `server/.sync-token`, or auto-generated on first launch.
* **Hardening:** 8 MiB request body cap, timing-safe comparison, no wildcard CORS, security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control: no-store`).

### 4.7 Session Handling

`data/SessionManager.kt` manages sign-in state:
* `saveSession(username, rememberMe)` — keeps the username in memory always; writes to `SharedPreferences` only when "Remember me" is on.
* 10-minute inactivity timeout: session expires if the app has been in the background for ≥ 10 minutes.
* `clearSession()` wipes memory and preferences.

---

## 5. User Guide — Guardian Role

The Guardian shell has five bottom-navigation tabs: **Home**, **Payments**, **Attendance**, **Security**, **More**.

### 5.1 Home (Dashboard)

* Greeting by time of day (Good morning / afternoon / evening) with your name and house ("Casa Marigold").
* Stat cards: Fully Paid, Partially Paid, Present Today, Action Needed (pending + overdue).
* Monthly collection progress bar showing collected vs expected.
* Quick actions grouped into:
  * **Notices:** Announcements, Leave Notices
  * **Records:** Payments, Attendance
  * **People & Safety:** Boarders, Security
  * **Emergency:** Emergency Contacts
* Tap your avatar to open the **Guardian Account** modal (name, username, house, boarder count, monthly collected, receipts to review, present today, sign out).

### 5.2 Payments

* View all boarders' payment records for the current period.
* Configure **Billing Settings**: due day, grace period, penalty type/amount, reminder schedule (before 3/1 day, on day, after 1/3 days), and channels (in-app, SMS, email).
* Guardian reviews boarder-submitted receipts:
  * **Pending** receipts can be verified (counts toward balance) or rejected (adjustes outstanding balance).
  * A payment counts as paid only after guardian verification.
* Record payments as Cash, GCash, Bank Transfer, or Online.

### 5.3 Attendance

* View attendance sessions (morning/evening) generated from the worship schedule.
* Check boarders in per session using QR or manual method.
* Sessions enforce a check-in window and QR validity deadline; scans outside the window are rejected.
* Send **absence alerts** to guardians of boarders who miss sessions beyond the threshold.

### 5.4 Security

* Manage **boarder accounts**: create boarder records, set ID numbers and passwords.
* Log **incidents** (missed curfew, SOS, missed worship, maintenance, other).
* File **maintenance reports** (plumbing, electrical, furniture, appliance, other).
* Manage **rooms** (name, floor, max capacity) and **jobs**.
* View **guardian profile** and emergency contacts.
* Deep-link from dashboard to boarder payments or attendance.

### 5.5 More

* Save/edit **rooms** and **jobs**.
* Configure **billing settings** and **worship schedule**.
* **Sign out**.

---

## 6. User Guide — Boarder Role

The Boarder shell mirrors the guardian experience from the boarder's perspective:

* View announcements addressed to you (or all boarders).
* Submit **payments** for the current period, optionally attaching a receipt image (saved to app storage; only the path is shared during sync).
* **Check in** to attendance sessions using your static QR code within the valid window.
* Submit **leave notices** with reason, destination, dates, and notes. Track status: Pending → Approved/Rejected → Departed → Completed (Arrived).
* View your profile and guardian/house information.

---

## 7. Shared Screens

### 7.1 Sign In

1. Launch the app. You land on the sign-in/registration screen.
2. Select your role (**Guardian** or **Boarder**).
3. Enter your credentials and tap **Sign in**.
4. The **"Remember me"** checkbox controls whether your session persists across a cold app start. If unchecked, the session is kept in memory only and cleared when the app is fully closed.
5. Sessions expire after **10 minutes of inactivity** (app in the background).

### 7.2 Wireless Sync

1. Open **Wireless Sync**.
2. Enter the server address, e.g. `http://192.168.1.42:3000`.
3. Paste the access token under **Server Access Token**.
4. Tap **Save Address** or **Sync Now**.

### 7.3 Statuses You'll See

| Field | Values |
|-------|--------|
| Payment status | Paid, Partial, Pending, Overdue |
| Receipt status | Pending review, Verified, Rejected |
| Leave notice | Pending, Approved, Rejected, Departed, Completed (Arrived) |
| Attendance | Present, Absent |
| Attendance method | QR, Manual |
| Session type | Morning, Evening |
| Announcement priority | Normal, Critical |
| Maintenance | Open, In progress, Resolved |
| Incident type | Missed curfew, SOS, Missed worship, Maintenance, Other |
| Curfew | Compliant, Late, Absent, Pending |
| Payment method | Cash, GCash, Bank Transfer, Online |

---

## 8. Synchronization

MBHM supports two sync modes:

1. **Wireless sync** — Android app ↔ computer running the Node.js sync server over the local network.
2. **Direct phone-to-phone sync** — guardian phone hosts, other devices join over Wi-Fi using a pairing token.

### 8.1 What Syncs

All Room collections are synchronized:

users, boarders, paymentRecords, attendanceSessions, attendanceRecords, announcements, visitorEntries, incidents, maintenanceReports, curfewRecords, worshipSchedules, billingSettings, leaveNotices, absenceAlerts, rooms, jobs, guardianProfile

### 8.2 What Does NOT Sync

* **Receipt image files:** only the stored file path (`receiptPath`) is synced. The image itself stays on the submitting device. A guardian on another device may see the filename without a preview.
* **Local preferences and tokens:** server access token, host address, client id, and session state are device-local and are not transferred.
* **Android Keystore-backed secrets:** the token is encrypted per-device and never leaves the device.

### 8.3 Wireless Sync — Server Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/` | HTML dashboard (auto-refreshes every 10s) |
| GET | `/api/status` | Service info + per-collection record counts |
| GET | `/api/export` | Full snapshot of all live records |
| GET | `/api/pull?since=<version>` | Incremental changes since `since` |
| POST | `/api/push` | Client uploads upserts/deletes; server resolves version conflicts |

### 8.4 Authentication

* `Authorization: Bearer <access-token>`
* Or `Basic mbhm:<access-token>` (username fixed to `mbhm`, password is the token)

The server validates with a timing-safe comparison. Unknown or missing credentials receive `401`.

### 8.5 Auto-Sync

`SyncManager` runs a background loop every **5 seconds**. It auto-syncs when auto-sync is enabled, a host address is set, a server access token is stored, and no sync is already in progress. Manual sync is triggered from the **Wireless Sync** screen via **Sync Now**.

### 8.6 Phone-to-Phone Sync

**Host (guardian phone):**
1. Open **Wireless Sync** and tap **Start Sharing**.
2. The phone displays its IP address and a temporary **pairing token**.
3. Share the IP and token only with the intended device.
4. Boarder devices can join a host but **cannot host** the shared database.
5. Tap **Stop Sharing** when finished. This invalidates the pairing token.

**Joining device:**
1. Enter the host's IP address and the pairing token.
2. Tap **Sync With Device**.

---

## 9. Sync Server API Reference

**Base URL:** `http://<computer-ip>:3000` (port configurable via `PORT` env, default `3000`)

### Authentication

All endpoints except the root dashboard require authentication:

```
Authorization: Bearer <access-token>
Authorization: Basic mbhm:<access-token>
```

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | HTML dashboard (auto-refreshes every 10s) |
| GET | `/api/status` | Service info + per-collection live record counts |
| GET | `/api/export` | Full snapshot of all live records |
| GET | `/api/pull?since=<version>` | Incremental changes since `since` |
| POST | `/api/push` | Client uploads upserts/deletes with version-conflict resolution |

### Error Responses

| Status | Meaning |
|--------|---------|
| 400 | Invalid JSON in request body |
| 401 | Authentication required |
| 404 | Unknown endpoint |
| 413 | Request body too large (> 8 MiB) |
| 500 | Request failed (server error) |

All responses include: `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Connection: close`.

---

## 10. Database Schema

MBHM uses **Room (SQLite)** for local persistence on the Android device. The same collections are mirrored on the Node.js sync server and exchanged via push/pull synchronization.

### 10.1 Tables

| Table | Primary Key | Notes |
|-------|-------------|-------|
| users | `username` | Sign-in accounts |
| boarders | `id` | Boarder profiles and contacts |
| rooms | `name` | Rooms managed by the guardian |
| jobs | `name` | Job/occupation options |
| guardian_profile | `id` ("default") | Guardian house profile |
| payment_records | `id` | Payment records with receipt status |
| attendance_sessions | `id` | Morning/evening check-in windows |
| attendance_records | `id` | Per-boarder, per-session attendance |
| announcements | `id` | Guardian-posted announcements |
| visitor_entries | `id` | Visitor log |
| incidents | `id` | SOS, missed curfew, maintenance, etc. |
| maintenance_reports | `id` | Maintenance tickets |
| curfew_records | `id` | Curfew check-ins |
| worship_schedule | `id` ("default") | Morning/evening schedule & thresholds |
| billing_settings | `id` ("default") | Due dates, penalties, reminders |
| leave_notices | `id` | Boarder leave requests |
| absence_alerts | `id` | Absence alerts sent to guardians |

### 10.2 Key Relationships

* Attendance records → boarders + sessions (`boarderId`, `sessionId`); unique index on `(boarderId, sessionId)`.
* Payments → boarders (`boarderId`).
* Leave notices → boarders (`boarderId`).
* Incidents / maintenance / curfew / absence alerts → boarders (incidents allow `null`).
* Announcements → recipients (JSON array of boarder IDs; `null` means all boarders).
* Guardian profile: single row (`id = "default"`), shared with all boarders.
* Worship schedule / billing settings: single-row tables (`id = "default"`).

### 10.3 Entity ↔ Domain Mapping

Each Room entity implements `toDomain()` (entity → domain model) and a `fromDomain` companion (domain → entity). This keeps UI and business logic free of Room annotations. `data/converter/Converters.kt` holds Room type converters for enums and lists.

---

## 11. Security

⚠️ **This is a student/demo build, not a production deployment.** Review the limitations below before using MBHM with real sensitive records.

### 11.1 Known Limitations

| Area | Limitation |
|------|-----------|
| Transport | Both sync modes use **plain HTTP** over the local network. Authentication does **not** encrypt network traffic. |
| Token scope | The sync token protects server endpoints but is **not per-user authorization**. All authenticated clients can access all shared collections. |
| Boarder QR | The boarder QR is a **static account identifier**, not proof of live presence. |
| Receipt images | Image files stay on the submitting device; only the path is synced. |
| SOS alerts | SOS incidents are recorded locally and shared on successful sync; there is **no live push or SMS delivery**. |
| Session | Sessions persist across cold starts only when "Remember me" is enabled. 10-minute inactivity timeout applies. |

### 11.2 Hardening Already Applied

* Token authentication on the sync server.
* Access-token field in Wireless Sync; token stored encrypted using Android Keystore.
* Pairing token for phone-to-phone sync (invalidated when sharing stops).
* Request/response hardening headers; removed wildcard CORS.
* Maximum server request body reduced to 8 MiB; direct-sync request bodies limited.
* Payment accounting fix: boarder-submitted payments count only after guardian verifies the receipt.
* Receipt images saved to app storage (path synced, not the file).
* Attendance check-in window enforced; static QR not accepted outside the window.
* "Remember me" controls session persistence across cold start.
* SOS confirmation copy corrected (no false instant SMS/push claims).
* Android backup disabled.
* Local server database, token, Gradle caches, and build output excluded from source archive.

### 11.3 Server Operator Guidance (Windows)

* The server listens on `0.0.0.0:3000`, reachable from any device on the LAN.
* Restrict any **Windows Firewall allowance to a private network** — do not expose the port on public networks.
* Avoid public Wi-Fi when syncing.
* Keep `server/.sync-token` private. If lost, stop the server, delete `.sync-token`, and restart to generate a new one; enter the new token on each phone.

### 11.4 Recommended Production Improvements

1. HTTPS/TLS for all sync traffic.
2. Per-user authorization (scope collections by account/house).
3. Full security review before handling real student/boarding data.
4. Encrypted local database (e.g., SQLCipher).
5. Push/SMS alert delivery for SOS and critical incidents.
6. Signed receipt image transfer or object-storage sync for payment proofs.

---

## 12. Contributing

### 12.1 Setup

1. Fork and clone the repository.
2. Open the `MBHM` folder in Android Studio.
3. Allow Gradle sync and install the Android SDK for compileSdk 37.
4. Build: `./gradlew :app:assembleDebug`
5. Run tests: `./gradlew test`

### 12.2 Code Style

* **Language:** Kotlin. UI built with **Jetpack Compose** and **Material 3**.
* **Theme:** Follow the existing blue-and-white brand palette in `ui/theme/Color.kt` and typography in `ui/theme/Type.kt`. Reuse shared components in `ui/components/`.
* **Architecture:** UI → `AppRepository` → Room (entities + DAOs). Domain models in `model/`. Sync logic in `data/sync/`.
* **Naming:** Files and classes use PascalCase; functions and variables use camelCase.

### 12.3 Adding a New Feature

1. **Domain model:** Add a data class in `model/Models.kt`.
2. **Room entity:** Add an `@Entity` data class in `data/entity/` with `toDomain()`/`fromDomain()` converters.
3. **DAO:** Add query methods in the appropriate DAO under `data/dao/`.
4. **Database:** Register the entity in `AppDatabase.kt`; bump the version and add a migration if needed.
5. **Repository:** Add observation and write methods to `AppRepository.kt`.
6. **UI:** Add Compose screens under `guardian/` and/or `boarder/`, and register them in the role shell's bottom navigation.
7. **Tests:** Add unit tests under `app/src/test/` and/or instrumented tests under `app/src/androidTest/`.

### 12.4 Testing

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### 12.5 Pull Request Process

1. Create a feature branch from `main`.
2. Make focused, atomic commits.
3. Write or update tests.
4. Update documentation if your change affects setup, architecture, sync, API, database, security, or user guide.
5. Open a pull request with a clear description.

---

## 13. Changelog

### Unreleased

**UI/UX Redesign — Modern Blue and White**
* Replaced former indigo/slate palette with consistent blue-and-white brand palette and stable Material 3 colors.
* Updated typography hierarchy, surface/background contrast, button treatment, rounded card geometry, and subtle elevation.
* Redesigned five-item bottom navigation with clearer active state and improved tap targets.
* Refined page headers, back controls, input fields, dropdowns, settings rows, table headers, state chips, and empty states.
* Updated sign-in/registration field styling.
* Added blue welcome panels to Guardian and Boarder dashboards.
* Updated legacy Android resource accent colors to match the new brand palette.

**Security & Sync Hardening**
* Added token authentication to the computer-based sync server.
* Added access-token field to Wireless Sync (Android Keystore storage, Bearer token).
* Added pairing token to direct phone-to-phone sync.
* Added request/response hardening headers; removed wildcard CORS; reduced max server request body to 8 MiB.

**Accounting & Data Fixes**
* Corrected payment accounting: boarder-submitted payments count only after guardian verifies the receipt.
* Saved selected receipt images into app storage (path synced, image file not synced).

**Attendance & Session Fixes**
* Enforced attendance check-in start and QR-validity deadline.
* Made "Remember me" control session persistence across cold app start.

**SOS & Privacy Fixes**
* Corrected SOS confirmation copy (records SOS incident, shares after sync; no instant SMS/push claim).
* Disabled Android app backup.
* Excluded local server database and token, Gradle caches, and build output from source archive.

---

## End of Document

*To save this document as a Microsoft Word file: open this file in Microsoft Word, then use **File → Save As** and choose **Word Document (.docx)**.*
