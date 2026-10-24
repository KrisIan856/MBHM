# Getting Started — MBHM

MBHM (Boarding House Management Hub) is an Android app for managing a boarding house, with two user roles: **Guardian** and **Boarder**. It includes attendance/QR check-in, payments, announcements, room management, leave notices, maintenance reports, incidents, and device synchronization.

This guide walks a developer through running the app and the sync server for the first time.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | Latest stable | Used to open and build the project |
| JDK | 17 | Required by Gradle / Android Gradle Plugin |
| Android SDK | compileSdk 37 | Specified in `app/build.gradle.kts` |
| Node.js | 22.5+ | Only needed to run the computer sync server |
| Git | Any | For cloning the repository |

---

## 1. Clone and open the project

```bash
git clone <repository-url>
cd MBHM
```

Open the `MBHM` folder in Android Studio. Allow Gradle sync to complete. The project uses Kotlin + Jetpack Compose with Room for local storage.

> The source archive excludes `local.properties`, Gradle caches, and generated build output. Android Studio will regenerate these on first sync/build.

---

## 2. Build and run the Android app

### From Android Studio

1. Select the `app` run configuration.
2. Choose a connected device or emulator (API level matching compileSdk 37 recommended).
3. Click **Run**.

### From the command line

```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug
```

The first build downloads dependencies and may take several minutes.

---

## 3. Run the computer sync server (Windows)

The sync server is a small Node.js HTTP server in the `server/` folder. It exposes a local dashboard and sync endpoints (`/api/status`, `/api/export`, `/api/pull`, `/api/push`) that the Android app connects to over the LAN.

1. Install **Node.js 22.5 or newer**.
2. Open the `server` folder and double-click `start-server.bat` (or run `node server.js`).
3. On first launch, the script creates `server/.sync-token` and starts the server on port 3000. The token is **not printed**; open the file locally when pairing a phone. Keep it private — it persists across restarts and is excluded from Git.
4. Find the computer's IPv4 or Tailscale address in the server window.
5. Open a browser at `http://<computer-ip>:3000` for the shared-database dashboard. Login is username `mbhm` with the access token as the password.

If the token is exposed or lost, stop the server, replace `server/.sync-token` with a new random value, restart, and enter the new token on each phone. Do not reuse the old token.

---

## 4. Connect the app to the server

1. In MBHM, open **Wireless Sync**.
2. Under **Server Address**, enter the computer's IPv4 or Tailscale address (e.g. `http://192.168.1.42:3000`). If you use Tailscale Serve or another TLS proxy, enter an `https://` address; the app defaults to port 443 when no port is given.
3. Under **Server Access Token**, paste the contents of `server/.sync-token` from the computer.
4. Tap **Save Address** or **Sync Now**.

The app stores the token encrypted using Android Keystore and sends it as a Bearer token on each request.

### Direct phone-to-phone sync

1. On the guardian phone, open **Wireless Sync** and tap **Start Sharing**. Boarder devices can join a host but cannot host.
2. Share the host phone's IP address and the displayed pairing token with the intended device only.
3. On the other phone, enter the host IP and token, then tap **Sync With Device**.
4. Tap **Stop Sharing** when finished. The pairing token is invalidated when sharing stops.

---

## 5. Run tests

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (requires a connected device/emulator)
./gradlew connectedAndroidTest
```

Test sources live under `app/src/test/` (unit) and `app/src/androidTest/` (instrumented).

---

## 6. Project structure

```
MBHM/
├── app/                    # Android app module
│   ├── src/main/
│   │   ├── java/com/example/mbhm/
│   │   │   ├── App.kt                  # Compose root, auth + role routing
│   │   │   ├── MainActivity.kt
│   │   │   ├── MBHMApplication.kt
│   │   │   ├── guardian/               # Guardian UI shell & screens
│   │   │   ├── boarder/                # Boarder UI shell & screens
│   │   │   ├── ui/auth/                # Sign-in / registration
│   │   │   ├── ui/components/          # Shared Compose components
│   │   │   ├── ui/theme/               # Material 3 theme (colors, type)
│   │   │   ├── model/                  # Domain data classes
│   │   │   ├── data/                   # Repository, database, DAOs, entities
│   │   │   └── data/sync/              # Sync manager, clients, token store
│   │   └── res/                        # Resources, manifest
│   └── src/test/                     # Unit tests
│   └── src/androidTest/              # Instrumented tests
├── server/                   # Node.js sync server
│   ├── server.js
│   ├── package.json
│   ├── start-server.bat
│   └── .sync-token             # generated on first run (excluded from Git)
├── docs/                     # this documentation
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Next steps

- Read [ARCHITECTURE.md](ARCHITECTURE.md) for a breakdown of the app layers and data flow.
- Read [SYNC.md](SYNC.md) for details on wireless and phone-to-phone synchronization.
- Read [SECURITY.md](SECURITY.md) for known limitations and hardening recommendations.
- Read [USER_GUIDE.md](USER_GUIDE.md) for a feature walkthrough of each role.