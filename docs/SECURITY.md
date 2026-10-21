# Security — MBHM

⚠️ **This is a student/demo build, not a production deployment.** Review the limitations below before using MBHM with real sensitive records.

---

## 1. Threat model & known limitations

| Area | Limitation | Recommendation |
|------|-----------|----------------|
| Transport | Both sync modes use **plain HTTP** over the local network. Authentication does **not** encrypt network traffic. | Use a trusted private Wi-Fi network only. Keep server ports off the public internet. Add HTTPS/TLS for production. |
| Token scope | The sync token protects server endpoints but is **not per-user authorization**. All authenticated clients can access all shared collections. | Add account/house-based access controls before production use. |
| Boarder QR | The boarder QR is a **static account identifier**. It is not proof of live presence, though scans are rejected outside the attendance window. | Do not treat the QR as live-presence proof. |
| Receipt images | Image files stay on the submitting device. Only the stored path is synced; a guardian on another device may see the filename without a preview. | Transfer images out-of-band or add encrypted file sync if needed. |
| SOS alerts | SOS incidents are recorded locally and shared on successful sync. There is **no live push or SMS delivery** in this build. | Do not rely on MBHM for emergency alerting. |
| Session | Sessions persist across cold starts only when "Remember me" is enabled; otherwise a cold start clears the session. There is a 10-minute inactivity timeout. | Treat as best-effort; add stronger session controls if needed. |

## 2. Hardening already applied

- **Token authentication** on the sync server — server status, push, pull, export, and the dashboard require a valid token (`Bearer` or `Basic mbhm:<token>`).
- **Access-token field in Wireless Sync** — the Android app stores the token encrypted using Android Keystore (`SecureTokenStore`) and sends it as a Bearer token.
- **Pairing token for phone-to-phone sync** — the host generates a temporary token when sharing starts; the joining device must enter it. Stopping sharing invalidates the token.
- **Request/response hardening headers** — `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`.
- **Removed wildcard CORS**.
- **Reduced maximum request body** to 8 MiB on the server; direct-sync request bodies also limited.
- **Payment accounting fix** — boarder-submitted payments count toward the balance only after the guardian verifies the receipt. Pending and rejected proofs no longer look paid; rejection changes the outstanding balance.
- **Receipt images saved to app storage** instead of storing only a filename.
- **Attendance check-in window enforced** — scans outside the start/QR-validity window are rejected; the static boarder QR is not accepted before or after the window.
- **"Remember me"** controls whether the session is persisted across a cold app start.
- **SOS confirmation copy corrected** — the app records an SOS incident and shares it after sync; it does not falsely claim instant SMS/push delivery.
- **Android backup disabled** to reduce accidental exposure of local records and secrets.
- **Local server database, token, Gradle caches, and build output excluded** from the source archive.

## 3. Server operator guidance (Windows)

- The server listens on `0.0.0.0:3000` by default, reachable from any device on the LAN.
- Restrict any **Windows Firewall allowance to a private network** — do not expose the port on public networks.
- Avoid public Wi-Fi when syncing.
- Keep the `server/.sync-token` file private. If lost, stop the server, delete `.sync-token`, and restart to generate a new one; enter the new token on each phone.
- The dashboard is at `http://<computer-ip>:3000` and refreshes every 10 seconds.

## 4. Recommended production improvements

1. **HTTPS/TLS** for all sync traffic (terminate at a reverse proxy or use a self-signed CA on the LAN).
2. **Per-user authorization** — scope collections and records by account/house instead of shared access for all authenticated clients.
3. **Full security review** before handling real student/boarding data.
4. **Encrypted local database** (e.g., SQLCipher) if the device may be lost or shared.
5. **Push/SMS alert delivery** for SOS and critical incidents.
6. **Signed receipt image transfer** or object-storage sync for payment proofs.

---

## Related docs

- [SYNC.md](SYNC.md) — sync architecture and endpoints
- [API.md](API.md) — server endpoint reference
- [README.md](../README.md) — server setup and build instructions