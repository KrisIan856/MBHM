# API Reference — MBHM Sync Server

The sync server is a single-file Node.js HTTP server (`server/server.js`) that runs on a computer on the local network. The Android app connects to it over wireless sync to push local changes and pull remote changes.

**Base URL**: `http://<computer-ip>:3000` (port configurable via `PORT` env, default `3000`). The Android client also accepts an `https://` reverse-proxy or Tailscale Serve address; with no explicit port it uses 443.

---

## Authentication

All endpoints except the root dashboard require authentication. Two schemes are accepted:

```
Authorization: Bearer <access-token>
Authorization: Basic mbhm:<access-token>
```

- The access token is stored in `server/.sync-token` (generated on first launch) or the `SYNC_TOKEN` environment variable.
- Username for Basic auth is fixed to `mbhm`; the password is the access token.
- Validation uses a timing-safe comparison.
- Missing/invalid credentials → `401` with `WWW-Authenticate` header.

---

## Endpoints

### GET /

Returns the shared-database HTML dashboard. Auto-refreshes every 10 seconds. Lists every collection as a table with row counts and LAN addresses devices can connect to.

- Requires auth: Yes (Basic or Bearer)
- Response: `text/html`

### GET /api/status

Service information and per-collection live record counts.

```json
{
  "ok": true,
  "service": "mbhm-sync-server",
  "version": 142,
  "records": 312,
  "tables": {
    "users": 2,
    "boarders": 12,
    "paymentRecords": 48,
    ...
  }
}
```

### GET /api/export

Full snapshot of all live (non-deleted) records across every collection.

```json
{
  "version": 142,
  "deviceName": "server",
  "timestamp": 1719000000000,
  "users": [ ... ],
  "boarders": [ ... ],
  "paymentRecords": [ ... ],
  ...
}
```

The `users` collection masks `passwordHash` as `••••••••` in the dashboard, but export returns raw stored data.

### GET /api/pull?since=<version>

Incremental changes since a given server version. Returns records with `version > since`, grouped by collection into `upserts` and `deletes`.

```json
{
  "since": 100,
  "version": 142,
  "upserts": {
    "timestamp": 1719000000000,
    "deviceName": "server",
    "boarders": [ { "id": "b1", ... } ],
    "paymentRecords": [ ... ]
  },
  "deletes": {
    "incidents": [ "inc-9" ]
  }
}
```

- If `since` is missing, zero, or greater than the current version, it is treated as `0` (full pull).
- Client should store the returned `version` as its next `since` marker.

### POST /api/push

Client uploads upserts and deletes. Body:

```json
{
  "clientId": "<device-client-id>",
  "payload": {
    "boarders": [ { "id": "b1", "name": "...", ... } ],
    "paymentRecords": [ ... ]
  }
}
```

Response:

```json
{
  "ok": true,
  "version": 145,
  "changes": 7,
  "conflicts": 1
}
```

Conflict semantics:
- If a record doesn't exist on the server → inserted with a new version (`changes++`).
- If it exists and the client's known version is older → **conflict**, server keeps its version (`conflicts++`, not applied).
- If data is identical → no change.
- Records the client previously knew but omits from the push → soft-deleted on the server (`deleted = 1`).

- Max request body: 8 MiB. Larger payloads → `413 Payload Too Large`.
- Invalid JSON → `400`.

---

## Error responses

All errors return JSON with `ok: false` and an `error` field:

| Status | Meaning |
|--------|---------|
| 400 | Invalid JSON in request body |
| 401 | Authentication required (missing or wrong token) |
| 404 | Unknown endpoint |
| 413 | Request body too large (> 8 MiB) |
| 500 | Request failed (server error) |

All responses include: `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Connection: close`.

---

## Collections synced

| Collection | Primary key |
|------------|-------------|
| users | `username` |
| boarders | `id` |
| paymentRecords | `id` |
| attendanceSessions | `id` |
| attendanceRecords | `id` |
| announcements | `id` |
| visitorEntries | `id` |
| incidents | `id` |
| maintenanceReports | `id` |
| curfewRecords | `id` |
| worshipSchedules | `id` |
| billingSettings | `id` |
| leaveNotices | `id` |
| absenceAlerts | `id` |
| rooms | `name` |
| jobs | `name` |
| guardianProfile | `id` |

---

## Client integration (Android)

`SyncManager` / `RemoteSyncClient` in `app/src/main/java/com/example/mbhm/data/sync/` handles:
- Storing the bearer token in Android Keystore (`SecureTokenStore`).
- Sending `clientId` (persisted per device) with every push.
- Tracking `since_version` in `SharedPreferences` for incremental pulls.
- Auto-syncing every 30 seconds when host, token, and network are available.

See [SYNC.md](SYNC.md) for the full sync flow.

---

## Related docs

- [SYNC.md](SYNC.md) — wireless and phone-to-phone synchronization
- [DATABASE.md](DATABASE.md) — Room schema and collection reference
- [SECURITY.md](SECURITY.md) — auth and transport limitations