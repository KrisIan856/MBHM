# Changelog — MBHM

All notable changes to MBHM are documented in this file.

---

## Unreleased

### UI/UX redesign — modern blue and white
- Replaced the former indigo/slate palette with a consistent blue-and-white brand palette and stable Material 3 colors.
- Updated typography hierarchy, surface/background contrast, button treatment, rounded card geometry, and subtle elevation across shared components.
- Redesigned the five-item bottom navigation with a clearer active state and improved tap targets.
- Refined page headers, back controls, input fields, dropdowns, settings rows, table headers, state chips, and empty states.
- Updated sign-in/registration field styling and grouped role selection and credential entry into clearer panels.
- Added blue welcome panels to the Guardian and Boarder dashboards.
- Updated legacy Android resource accent colors to match the new brand palette.

> No intentional changes to database entities, Room schema, repository APIs, navigation destinations, or feature workflows were made for the redesign.

### Security & sync hardening
- Added token authentication to the computer-based sync server (status, push, pull, export, dashboard require a valid token).
- Added an access-token field to Wireless Sync; the Android app stores the token encrypted using Android Keystore and sends it as a Bearer token.
- Added a pairing token to direct phone-to-phone sync. A host generates a temporary token when sharing starts; the joining device must enter it. Stopping sharing invalidates the token.
- Added request/response hardening headers, removed wildcard CORS, reduced maximum server request body to 8 MiB, and limited direct-sync request bodies.

### Accounting & data fixes
- Corrected payment accounting: boarder-submitted payments count toward the balance only after the guardian verifies the receipt. Pending and rejected proofs no longer look paid, and rejection now changes the outstanding balance.
- Saved selected receipt images into app storage instead of storing only a filename. The image path is synced; the image file itself is not.

### Attendance & session fixes
- Enforced the attendance check-in start and QR-validity deadline. Scans outside the window are rejected; the static boarder QR is no longer accepted before or after the window.
- Made the "Remember me" checkbox control whether the session is persisted across a cold app start.

### SOS & privacy fixes
- Corrected SOS confirmation copy: the app records an SOS incident and shares it after successful sync; it does not falsely claim to send an instant SMS/push notification.
- Disabled Android app backup to reduce accidental exposure of local management records and secrets.

### Archive hygiene
- Excluded the local server database and token, Gradle caches, and build output from the source archive.

---

<sup>Older release history is not yet recorded. Contributors are encouraged to add versioned entries here as releases are cut.</sup>
