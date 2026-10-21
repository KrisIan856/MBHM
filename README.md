# MBHM — Boarding House Management Hub

MBHM is an Android app for boarding-house management with Guardian and Boarder interfaces. Existing modules include attendance/QR check-in, payments, announcements, room management, leave notices, maintenance reports, incidents, and device synchronization.

## UI/UX redesign — modern blue and white

This copy includes a visual refresh across the shared Android interface and both user roles. The database entities, Room schema, repository APIs, navigation destinations, and feature workflows were not intentionally changed for the redesign.

- Replaced the former indigo/slate palette with a consistent blue-and-white brand palette and stable Material 3 colors.
- Updated typography hierarchy, surface/background contrast, button treatment, rounded card geometry, and subtle elevation across shared components.
- Redesigned the role-specific bottom navigation with a clearer active state and improved tap targets.
- Refined page headers, back controls, input fields, dropdowns, settings rows, table headers, state chips, and empty states.
- Updated sign-in / registration field styling and grouped the sign-in role selection and credential entry into clearer panels.
- Added blue welcome panels to the Guardian and Boarder dashboards while preserving the existing profile actions, information, and navigation.
- Updated legacy Android resource accent colors to match the new brand palette.

## Changes in this improved copy

- Added token authentication to the computer-based sync server. Server status, push, pull, export, and the dashboard require a valid token.
- Added an access-token field to **Wireless Sync**. The Android app stores that token encrypted using Android Keystore and sends it as a Bearer token.
- Added a pairing token to direct phone-to-phone sync. A host generates a temporary token when sharing starts; the other device must enter it to sync. Stopping sharing invalidates the token.
- Added request/response hardening headers, removed wildcard CORS, reduced maximum server request bodies to 8 MiB, and limited direct-sync request bodies.
- Corrected payment accounting: boarder-submitted payments count toward the balance only after the guardian verifies the receipt. Pending and rejected proofs no longer look paid, and rejection now changes the outstanding balance.
- Saved selected receipt images into app storage instead of storing only a filename. The image path is synced; the image file itself is not, so a guardian on another device may see the filename without a preview.
- Enforced the attendance check-in start and QR-validity deadline. Scans outside the window are rejected; the static boarder QR is no longer accepted before or after the window.
- Made the "Remember me" checkbox control whether the session is persisted across a cold app start.
- Added nested Android back handling so system back unwinds sub-screens (for example Settings → More main → Home) instead of jumping straight to Home.
- Added a Guardian **Boarders** tab that combines the roster (room assignments, contact details, current-month payment standing, and search) with boarder account creation, room/job edits, and password resets. Boarder management was moved out of **Security & Emergency**.
- Corrected SOS confirmation copy: the app records an SOS incident and shares it after successful sync; it does not falsely claim to send an instant SMS/push notification.
- Disabled Android app backup to reduce accidental exposure of local management records and secrets.
- Excluded the local server database and token, Gradle caches, and build output from this source archive.

## Computer sync server setup (Windows)

1. Install **Node.js 22.5 or newer**.
2. Open the `server` folder and double-click `start-server.bat`.
3. On first launch, the script creates `server/.sync-token`, prints the access token, and starts the server. Keep the token private. The file persists the same token across restarts and is excluded from Git.
4. Find the computer's IPv4 address in the server window.
5. In MBHM, open **Wireless Sync**. Enter that IP under **Server Address**, paste the printed token under **Server Access Token**, then tap **Save Address** or **Sync Now**.
6. To open the dashboard in a browser, visit `http://<computer-ip>:3000`. When prompted, use username `mbhm` and the access token as the password.

If the token is lost, stop the server, delete `server/.sync-token`, and restart `start-server.bat` to generate a new one. Enter the new token on each phone.

## Direct phone-to-phone sync

1. On the guardian phone, open **Wireless Sync** and tap **Start Sharing**. Boarder devices can join a host but cannot host the shared database.
2. Give the other device the host phone's IP address and the pairing token displayed under the sharing status. Share the token only with the intended device.
3. On the other phone, enter the host IP and token, then tap **Sync With Device**.
4. Tap **Stop Sharing** when finished. The pairing token is invalidated when sharing stops.

## Important security limitations

This is a student/demo build, not a production deployment. Both sync modes currently use plain HTTP on a local network; **authentication does not encrypt network traffic**, so a person able to monitor that network could capture a token or data. Use a trusted private Wi-Fi network only, keep the ports off the public internet, and do not use real sensitive records until HTTPS/TLS, access controls by account/house, and a full security review are in place. Restrict any Windows Firewall allowance to a private network. Avoid public Wi-Fi.

The token protects the sync server endpoints; it does not replace per-user authorization. All authenticated server clients currently have access to the shared collections. The boarder QR is still a static account identifier, so it should not be treated as proof of live presence, even though scans are now rejected outside the configured window. Receipt image files stay on the submitting device and are not transferred by database sync; only the stored path is shared. SOS incidents are recorded locally and shared on successful sync; there is no live push/SMS alert delivery in this build. Use unique, non-demo data while developing.

## Build and run the Android app

1. Open this folder in Android Studio.
2. Allow Gradle sync and install the Android SDK/platform required by `app/build.gradle.kts` (`compileSdk` 37 in this project).
3. Select the `app` run configuration and deploy to a device/emulator.

The source archive does not include `local.properties`, Gradle caches, or generated build output. Run `./gradlew :app:assembleDebug` (or build from Android Studio) and test on an emulator/device before distributing the APK.

## Documentation

Detailed documentation lives in the [`docs/`](docs/) folder:

| Document | Covers |
|----------|--------|
| [GETTING_STARTED.md](docs/GETTING_STARTED.md) | Developer onboarding: prerequisites, building, running the app and server |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | App layers, data flow, Room, sync server |
| [USER_GUIDE.md](docs/USER_GUIDE.md) | Feature walkthrough for Guardian and Boarder roles |
| [SYNC.md](docs/SYNC.md) | Wireless sync, phone-to-phone sync, endpoints, auto-sync |
| [API.md](docs/API.md) | Sync server endpoint reference (`/api/status`, `/api/pull`, `/api/push`, etc.) |
| [DATABASE.md](docs/DATABASE.md) | Room schema, tables, entity ↔ domain mapping |
| [SECURITY.md](docs/SECURITY.md) | Threat model, known limitations, hardening recommendations |
| [CONTRIBUTING.md](docs/CONTRIBUTING.md) | Code style, adding features, testing, pull requests |
| [CHANGELOG.md](docs/CHANGELOG.md) | Versioned change history |
