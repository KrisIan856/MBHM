# Security — MBHM

⚠️ **This is a student/demo build, not a production deployment.** Review the limitations below before using MBHM with real sensitive records.

---

## 1. Threat model & known limitations

| Area | Limitation | Recommendation |
|------|-----------|----------------|
| Transport | Wireless sync uses **plain HTTP by default**; direct sync also uses plain HTTP. The Android client accepts `https://` addresses (default port 443), but the Node server does not terminate TLS itself. Authentication does **not** encrypt traffic. | Use Tailscale/WireGuard or an HTTPS reverse proxy such as Tailscale Serve. Keep server ports off the public internet. |
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
- **SOS confirmation copy corrected** — the app records an SOS incident and shares it after sync; it does not falsely claim instant SMS/push delivery. The confirmation is shown only after the local incident insert completes.
- **Android backup disabled** to reduce accidental exposure of local records and secrets.
- **Sync token rotated on 2026-10-24.** The exposed token must no longer be used. The current token is stored only in `server/.sync-token` (untracked) and in Android Keystore on each paired phone. Re-enter it on every phone after rotation.
- **Server no longer prints the access token at startup.**
- **Source archives** — `.gitignore` excludes `.git`, `.gradle`, `.idea`, build output, `local.properties`, `server/.sync-token`, and `server/mbhm_server.db*`. Do not create a source ZIP by manually compressing the working folder. Use `scripts/package-source.ps1`, which archives Git HEAD and excludes tracked IDE/generated files, or `git archive` with explicit exclusions. Inspect the ZIP before sharing.

## 3. Server operator guidance (Windows)

- The server listens on `0.0.0.0:3000` by default, reachable from any device on the LAN or Tailscale network.
- Restrict any **Windows Firewall allowance to a private network** — do not expose the port on public networks and do not add router port forwarding.
- On Windows, allow **TCP 3000 inbound only on the Private profile** for `C:\Program Files\nodejs\node.exe` (or the Node executable you use). Do not enable the rule on the Public profile. A device can successfully ping the laptop while TCP 3000 is still blocked, so test the port rather than relying on ping.
- For remote access, use Tailscale on the laptop and each phone. Restrict the boarder phone's ACL to the laptop's sync port (`mbhm-laptop:3000`).
- Avoid public Wi-Fi when syncing.
- Keep the `server/.sync-token` file private. The server creates it on first launch but does **not** print it. Open the file locally only when pairing a phone.
- To rotate the token: stop the server, replace `server/.sync-token` with a new random value (do not reuse the old one), restart the server, then update the token on every phone. Devices with the old token receive `401` until updated.
- The dashboard is at `http://<computer-ip>:3000` and refreshes every 10 seconds. If you put the server behind an HTTPS proxy, use the HTTPS URL in the app; the app defaults to port 443 when no port is supplied.

## 4. Recommended production improvements

1. **HTTPS/TLS** for all sync traffic (Tailscale Serve or a reverse proxy with a trusted certificate). The Android client already accepts `https://`.
2. **Per-user authorization** — scope collections and records by account/house instead of shared access for all authenticated clients. Until this exists, every authenticated client can read the shared payload, including user records.
3. **Remove password hashes from sync payloads** and add a separate, auditable account-enrollment/reset flow.
4. **Full security review** before handling real student/boarding data.
5. **Encrypted local database** (e.g., SQLCipher) if the device may be lost or shared.
6. **Push/SMS alert delivery** for SOS and critical incidents.
7. **Signed receipt image transfer** or object-storage sync for payment proofs.

---

## Related docs

- [SYNC.md](SYNC.md) — sync architecture and endpoints
- [API.md](API.md) — server endpoint reference
- [README.md](../README.md) — server setup and build instructions