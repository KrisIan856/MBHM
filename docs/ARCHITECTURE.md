# Architecture — MBHM

MBHM is an Android application written in Kotlin using Jetpack Compose for the UI and Room (SQLite) for local persistence. A companion Node.js sync server runs on a computer on the same LAN, and devices can also sync directly phone-to-phone over Wi-Fi.

The app follows a clean separation between UI, domain, and data layers, with a single `AppRepository` acting as the source of truth.

---

## 1. High-level data flow

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

## 2. UI layer (`ui/`)

- **Theme** (`ui/theme/`): Material 3 color (`Color.kt`), typography (`Type.kt`), and component theme (`Theme.kt`). The project uses a consistent blue-and-white brand palette.
- **Components** (`ui/components/`): Shared Compose building blocks — `StatCard`, `PCard`, `EmptyState`, `ChipBar`, `DateRangeFilter`, `NavBar`, `NavItem`, `Modal`, `Toast`, QR display, money formatting, etc.
- **Auth** (`ui/auth/AuthScreen.kt`): Sign-in / registration screen with role selection and credential entry panels. "Remember me" controls session persistence across cold starts.

### Role shells

Each role is a self-contained Compose shell with a five-item bottom navigation:

| Role | Shell file | Tabs |
|------|-----------|------|
| Guardian | `guardian/GuardianApp.kt` | Dashboard, Payments, Attendance, Security, More |
| Boarder | `boarder/BoarderApp.kt` | Boarder-facing equivalents |

The root `App.kt` (`MbhmApp`) routes between `AuthScreen`, `GuardianApp`, and `BoarderApp` based on the signed-in user's `Role` (`GUARDIAN` / `BOARDER`). It also seeds sample data on first launch (`seedIfEmpty()`) and maintains attendance sessions on a 15-second tick.

## 3. Domain layer (`model/`)

Plain Kotlin data classes and enums, independent of Android and Room:

- `Role`, `PayStatus`, `ReceiptStatus`, `IncidentType`, `CurfewStatus`, `LeaveNoticeStatus`, `PayMethod`, `RecordedBy`, `AttendanceStatus`, `AttendanceMethod`, `SessionType`, `Priority`, `PenaltyType`, `MaintCategory`, `MaintStatus`
- `Boarder`, `PaymentRecord`, `AttendanceSession`, `AttendanceRecord`, `Announcement`, `VisitorEntry`, `Incident`, `MaintenanceReport`, `CurfewRecord`, `WorshipSchedule`, `AbsenceAlertRecord`
- `BillingSettings`, `ReminderSettings`, `ChannelSettings`, `GuardianProfile`, `GuardianDetails`, `HouseRoom`, `HouseJob`, `EmergencyContact`, `GuardianContact`, `NewBoarderInput`

## 4. Data layer (`data/`)

### Repository

`data/repository/AppRepository.kt` is a singleton (`getInstance(context)`) that exposes:
- Boarders, payments, attendance records/sessions, announcements, incidents, maintenance reports, visitor entries, leave notices, absence alerts, rooms, jobs, guardian profile, billing settings, worship schedule.
- `Flow`-based observation methods (`collectAsState` in UI) plus suspend upsert/update methods used by sync and UI actions.
- `seedIfEmpty()` to populate demo data on first launch.

### Room database

`data/database/AppDatabase.kt` defines the Room database with entities and DAOs under `data/entity/` and `data/dao/`:

| Table | Entity file | DAO |
|-------|-------------|-----|
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
| users | `UserEntity` | `UserDao` |

Each entity has `toDomain()` / `fromDomain()` converters between the Room entity and the domain model. `data/converter/Converters.kt` holds Room type converters for enums and lists.

## 5. Sync layer (`data/sync/`)

- **`SyncManager`** — singleton orchestrator. Holds host, auto-sync flag, bearer token, and client id in `SharedPreferences` (`mbhm_sync`). Runs auto-sync every 5 seconds when host, token, and network are available and not busy. Exposes a `StateFlow<SyncState>`.
- **`RemoteSyncClient`** — HTTP client that talks to the Node.js server (`push`, `pull`, `export`, `status`). Sends the bearer token stored in `SecureTokenStore`.
- **`SecureTokenStore`** — stores the server access token encrypted with Android Keystore.
- **`LocalSyncClient` / `LocalSyncServer`** — direct phone-to-phone sync over Wi-Fi using a pairing token.
- **`NetworkUtils`** — network reachability helpers.

## 6. Sync server (`server/`)

A single-file Node.js HTTP server (`server.js`) using the built-in `node:sqlite` module:

- **Database**: `mbhm_server.db` (WAL journal mode), one `records` table versioned by an incrementing `version` column, plus `client_rows` for per-client known versions and a `meta` table.
- **Collections**: users, boarders, paymentRecords, attendanceSessions, attendanceRecords, announcements, visitorEntries, incidents, maintenanceReports, curfewRecords, worshipSchedules, billingSettings, leaveNotices, absenceAlerts, rooms, jobs, guardianProfile.
- **Endpoints**:
  - `GET /` — HTML dashboard (auto-refreshes every 10s)
  - `GET /api/status` — service info + per-collection counts
  - `GET /api/export` — full snapshot of all live records
  - `GET /api/pull?since=<version>` — incremental changes since a version
  - `POST /api/push` — client upserts/deletes with version-conflict resolution (server keeps the newer version)
- **Auth**: `Authorization: Bearer <token>` or `Basic mbhm:<token>`. Token loaded from `SYNC_TOKEN` env, `server/.sync-token`, or auto-generated on first launch.
- **Hardening**: 8 MiB request body cap, timing-safe comparison, no wildcard CORS, security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Cache-Control: no-store`).

## 7. Session handling

`data/SessionManager.kt` manages sign-in state:
- `saveSession(username, rememberMe)` — keeps the username in memory always; writes to `SharedPreferences` only when "Remember me" is on.
- 10-minute inactivity timeout: session expires if the app has been in the background for ≥ 10 minutes.
- `clearSession()` wipes memory and preferences.

## 8. Build configuration

- **Kotlin** + **Jetpack Compose** + **Room** + **KSP** (configured in root `build.gradle.kts`)
- `compileSdk` 37
- Single app module (`:app`)
- Repositories: Google, Maven Central, Gradle Plugin Portal
- Build output, Gradle caches, the server database, and `.sync-token` are excluded from version control (see `.gitignore`).

---

## Related docs

- [SYNC.md](SYNC.md) — wireless and phone-to-phone synchronization details
- [API.md](API.md) — sync server endpoint reference
- [DATABASE.md](DATABASE.md) — Room schema reference
- [SECURITY.md](SECURITY.md) — security limitations and hardening