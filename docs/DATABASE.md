# Database — MBHM

MBHM uses **Room (SQLite)** for local persistence on the Android device. The same collections are mirrored on the Node.js sync server and exchanged via push/pull synchronization.

This document is the schema reference: tables, columns, key relationships, and the entity ↔ domain model mapping.

---

## 1. Entities and tables

| Table | Entity file | Primary key | Notes |
|-------|-------------|-------------|-------|
| `users` | `UserEntity` | `username` | Sign-in accounts |
| `boarders` | `BoarderEntity` | `id` | Boarder profiles and contacts |
| `rooms` | `RoomEntity` | `name` | Rooms managed by the guardian |
| `jobs` | `JobEntity` | `name` | Job/occupation options |
| `guardian_profile` | `GuardianProfileEntity` | `id` ("default") | Guardian house profile |
| `payment_records` | `PaymentRecordEntity` | `id` | Payment records with receipt status |
| `attendance_sessions` | `AttendanceSessionEntity` | `id` | Morning/evening check-in windows |
| `attendance_records` | `AttendanceRecordEntity` | `id` | Per-boarder, per-session attendance |
| `announcements` | `AnnouncementEntity` | `id` | Guardian-posted announcements |
| `visitor_entries` | `VisitorEntryEntity` | `id` | Visitor log |
| `incidents` | `IncidentEntity` | `id` | SOS, missed curfew, maintenance, etc. |
| `maintenance_reports` | `MaintenanceReportEntity` | `id` | Maintenance tickets |
| `curfew_records` | `CurfewRecordEntity` | `id` | Curfew check-ins |
| `worship_schedule` | `WorshipScheduleEntity` | `id` ("default") | Morning/evening schedule & thresholds |
| `billing_settings` | `BillingSettingsEntity` | `id` ("default") | Due dates, penalties, reminders |
| `leave_notices` | `LeaveNoticeEntity` | `id` | Boarder leave requests |
| `absence_alerts` | `AbsenceAlertEntity` | `id` | Absence alerts sent to guardians |

Entity and DAO source files live under `app/src/main/java/com/example/mbhm/data/entity/` and `data/dao/`. Room type converters are in `data/converter/Converters.kt`.

---

## 2. Key relationships

- **Attendance records → boarders + sessions**: `AttendanceRecordEntity.boarderId` references `boarders.id`; `AttendanceRecordEntity.sessionId` references `attendance_sessions.id`. A unique index exists on `(boarderId, sessionId)` so a boarder can be checked into a session only once.
- **Payments → boarders**: `PaymentRecordEntity.boarderId` references `boarders.id`.
- **Leave notices → boarders**: `LeaveNoticeEntity.boarderId` references `boarders.id`.
- **Incidents / maintenance / curfew / absence alerts → boarders**: each carries a `boarderId` (incidents allow `null`).
- **Announcements → recipients**: `recipients` is a JSON array of boarder IDs; `null` means all boarders.
- **Guardian profile**: single row with `id = "default"`, shared with all boarders.
- **Worship schedule / billing settings**: single-row tables with `id = "default"`.

Room enforces relationships via the repository layer rather than SQLite foreign keys in most cases; consult the DAO interfaces for exact constraints.

---

## 3. Entity ↔ domain mapping

Each Room entity implements `toDomain()` (entity → domain model) and a `fromDomain` companion (domain → entity). This keeps UI and business logic free of Room annotations.

Example (`BoarderEntity`):

```kotlin
fun toDomain(): Boarder = Boarder(
    id = id,
    name = name,
    room = room,
    // ...
    emergencyContact = EmergencyContact(
        name = emergencyContactName,
        phone = emergencyContactPhone,
        secondaryPhone = emergencyContactSecondaryPhone,
        address = emergencyContactAddress
    ),
    // ...
)

companion object {
    fun fromDomain(boarder: Boarder): BoarderEntity = BoarderEntity(
        id = boarder.id,
        name = boarder.name,
        // ...
    )
}
```

The same pattern applies to `PaymentRecordEntity`, `AttendanceRecordEntity`, `AnnouncementEntity`, `IncidentEntity`, `MaintenanceReportEntity`, `LeaveNoticeEntity`, `AbsenceAlertEntity`, `CurfewRecordEntity`, `WorshipScheduleEntity`, `BillingSettingsEntity`, `RoomEntity`, `JobEntity`, and `GuardianProfileEntity`.

---

## 4. Notable columns

### `boarders`
`name`, `room`, `floor`, `initials`, `avatarColor`, `phone`, `email`, `joinDate`, `monthlyRate`, `dueDay`, `gracePeriodDays`, `paymentStatus`, `latePenaltyType`, `latePenaltyAmount`, `emergencyContact*`, `guardianContact*`, `job`, `idNumber`

### `payment_records`
`boarderId`, `period`, `amount`, `paidDate`, `method`, `status` (`PayStatus`), `receiptStatus` (`ReceiptStatus`), `receiptFileName`, `receiptPath`, `rejectionReason`, `amountPaid`, `guardianReceiptFileName`, `recordedBy` (`RecordedBy`), `verifiedAt`, `verifiedBy`, `notes`

### `attendance_sessions`
`type` (`SessionType`: MORNING/EVENING), `date`, `windowStart`, `windowEnd`, `qrValidityMins`

### `attendance_records`
`boarderId`, `sessionId`, `status` (`AttendanceStatus`), `checkInTime`, `method` (`AttendanceMethod`: QR/MANUAL), `overrideReason`, `overriddenBy`

### `leave_notices`
`boarderId`, `leaveDate`, `expectedDepartureTime`, `expectedReturnDate`, `expectedReturnTime`, `reason`, `destination`, `additionalNotes`, `status` (`LeaveNoticeStatus`), `submittedAt`, `verifiedBy`, `verifiedAt`, `departedAt`, `arrivedAt`

---

## 5. Sync server schema mirror

The Node.js server (`server/server.js`) stores all collections in a single `records` table:

```sql
CREATE TABLE records (
    collection TEXT NOT NULL,
    record_id  TEXT NOT NULL,
    data       TEXT NOT NULL,      -- JSON-serialized domain object
    version    INTEGER NOT NULL,
    deleted    INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (collection, record_id)
);
```

A `client_rows` table tracks each client's known version per collection for incremental pull. Versions are monotonically increasing; on conflict (client version < server version), the server keeps its data.

---

## 6. Migrations

Room database version and migration strategy are defined in `AppDatabase.kt`. If you modify entities, bump the database version and add a migration, or clear app data during development. The sync server creates its tables on startup (`CREATE TABLE IF NOT EXISTS`) and runs a lightweight `ALTER TABLE ... ADD COLUMN` check for `client_rows.known_version`.

---

## Related docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — data layer overview
- [SYNC.md](SYNC.md) — how collections are synchronized
- [API.md](API.md) — server endpoints that expose these collections