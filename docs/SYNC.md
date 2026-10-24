# Synchronization — MBHM

MBHM supports two sync modes:

1. **Wireless sync** — Android app ↔ computer running the Node.js sync server over the local network.
2. **Direct phone-to-phone sync** — guardian phone hosts, other devices join over Wi-Fi using a pairing token.

Both modes sync the same set of Room collections and use version-based incremental synchronization.

---

## 1. What syncs

All Room collections are synchronized:

- users
- boarders
- paymentRecords
- attendanceSessions
- attendanceRecords
- announcements
- visitorEntries
- incidents
- maintenanceReports
- curfewRecords
- worshipSchedules
- billingSettings
- leaveNotices
- absenceAlerts
- rooms
- jobs
- guardianProfile

## 2. What does NOT sync

- **Receipt image files**: only the stored file path (`receiptPath`) is synced. The image itself stays on the submitting device. A guardian on another device may see the filename without a preview.
- **Local preferences and tokens**: server access token, host address, client id, and session state are device-local and are not transferred.
- **Android Keystore-backed secrets**: the token is encrypted per-device and never leaves the device.

---

## 3. Wireless sync (app ↔ computer server)

### Server setup

1. Install **Node.js 22.5 or newer**.
2. Open `server/` and run `start-server.bat` (Windows) or `node server.js`.
3. On first launch, `server/.sync-token` is created and the access token is written to that file. The token persists across restarts, is excluded from Git, and is **not printed** by the server.
4. Note the computer's IPv4 address or Tailscale address shown in the server window.

### Connect from the app

1. In MBHM, open **Wireless Sync**.
2. Enter the server address, e.g. `http://192.168.1.42:3000` on the LAN, or `http://100.x.y.z:3000` over Tailscale. If you front the server with Tailscale Serve or another TLS proxy, use `https://...`; the app defaults to port 443 when no port is supplied.
3. Open `server/.sync-token` on the computer and paste its contents under **Server Access Token**. Do not send the token through chat or email.
4. Tap **Save Address** or **Sync Now**.

The app stores the token encrypted using Android Keystore (`SecureTokenStore`) and sends it as a `Bearer` token on every request.

### Rotating the sync token

If a token may have been shared or archived:

1. Stop the server.
2. Replace `server/.sync-token` with a new random value. Do not reuse the old token.
3. Restart the server.
4. Enter the new token on every phone. Devices still using the old token receive `401` until updated.

The current token was rotated on 2026-10-24. The old token must not be used.

### Server endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/` | HTML dashboard (auto-refreshes every 10s) |
| GET | `/api/status` | Service info + per-collection record counts |
| GET | `/api/export` | Full snapshot of all live records |
| GET | `/api/pull?since=<version>` | Incremental changes since `since` |
| POST | `/api/push` | Client uploads upserts/deletes; server resolves version conflicts |

### Authentication

- `Authorization: Bearer <access-token>`
- Or `Basic mbhm:<access-token>` (username fixed to `mbhm`, password is the token)

The server validates with a timing-safe comparison. Unknown or missing credentials receive `401`.

### Sync algorithm (push / pull)

**Push** (`POST /api/push`): the client sends `clientId` and a `payload` of collections → rows. For each row:
- If the record doesn't exist on the server, it is inserted with a new version.
- If the data is identical to the server's current live row, nothing changes and the client's known version is acknowledged.
- If it exists, the data differs, and the client's known version is older, it's a **conflict** — the server keeps its version and does **not** advance the client's acknowledged version. The client must pull the canonical row and push again.
- Rows the client knows about but are missing from the push are soft-deleted on the server.

This acknowledgment rule prevents a client whose pull failed from overwriting a newer server row on the next push.

**Pull** (`GET /api/pull?since=`): the server returns all records with `version > since`, split into `upserts` and `deletes` per collection, plus the current server `version`. The client stores the returned version as its new `since` marker.

### Auto-sync

`SyncManager` runs a background loop every **30 seconds**. It auto-syncs when:
- `autoSync` is enabled,
- a host address is set,
- a server access token is stored,
- no sync is already in progress.

Manual sync is triggered from the **Wireless Sync** screen via **Sync Now**.

### Dashboard

Visit `http://<computer-ip>:3000` in a browser to view the shared database. Login: username `mbhm`, password = access token. Each collection is shown as a table with row counts.

---

## 4. Phone-to-phone sync

### Host (guardian phone)

1. Open **Wireless Sync** and tap **Start Sharing**.
2. The phone displays its IP address and a temporary **pairing token**.
3. Share the IP and token only with the intended device.
4. Boarder devices can join a host but **cannot host** the shared database.
5. Tap **Stop Sharing** when finished. This invalidates the pairing token.

### Joining device

1. Enter the host's IP address and the pairing token.
2. Tap **Sync With Device**.
3. Data is exchanged directly over the local Wi-Fi connection.

> ⚠️ **Current limitation:** direct sync is host-authoritative for records with matching IDs. It does not perform field-level merge or preserve the joining device's edits to a record the host also changed. Back up the joining device before syncing. A safer merge policy requires a protocol change.

---

## 5. Configuration keys

Stored in `SharedPreferences` (`mbhm_sync`) by `SyncManager`:

| Key | Meaning |
|-----|---------|
| `server_host` | Wireless sync server address |
| `auto_sync` | Whether automatic sync is enabled |
| `since_version` | Last server version pulled (incremental marker) |
| `client_id` | Unique per-device client id (auto-generated) |

The access token is stored separately in `SecureTokenStore` (Android Keystore), not in preferences.

---

## 6. Known limitations

- Wireless sync uses **plain HTTP by default**; direct sync also uses plain HTTP. The Android client accepts `https://` addresses, but the Node server does not terminate TLS itself. Authentication does **not** encrypt traffic — use Tailscale/WireGuard or an HTTPS reverse proxy, and avoid public Wi-Fi.
- The token protects sync endpoints but does **not** replace per-user authorization. All authenticated clients can read the shared collections. User records, including password hashes, are part of the shared sync payload until role-scoped sync is implemented.
- Direct sync is host-authoritative for matching record IDs and does not merge field-level edits.
- The boarder QR is a **static account identifier**, not proof of live presence (scans are rejected outside the configured attendance window, but the QR itself is static).
- Receipt image files are **not** transferred; only the path is shared.
- SOS incidents are recorded locally and shared on successful sync; there is **no live push or SMS alert delivery** in this build.

For production hardening recommendations, see [SECURITY.md](SECURITY.md).