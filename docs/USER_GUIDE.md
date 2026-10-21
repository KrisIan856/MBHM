# User Guide — MBHM

MBHM (Boarding House Management Hub) is an Android app for managing a boarding house. It has two user roles:

- **Guardian** — manages boarders, payments, attendance, announcements, rooms, leave notices, incidents, and maintenance.
- **Boarder** — views announcements, submits payments with receipts, checks in to attendance, and submits leave notices.

This guide walks through the core features for each role, plus shared screens like sign-in and sync.

---

## 1. Sign in

1. Launch the app. You land on the sign-in/registration screen.
2. Select your role (**Guardian** or **Boarder**).
3. Enter your credentials and tap **Sign in**.
4. The **"Remember me"** checkbox controls whether your session persists across a cold app start. If unchecked, the session is kept in memory only and cleared when the app is fully closed.
5. Sessions expire after **10 minutes of inactivity** (app in the background).

---

## 2. Guardian — role walkthrough

The Guardian shell has five bottom-navigation tabs: **Home**, **Payments**, **Attendance**, **Security**, **More**.

### Home (dashboard)

- Greeting by time of day (Good morning/afternoon/evening) with your name and house ("Casa Marigold").
- Stat cards: Fully Paid, Partially Paid, Present Today, Action Needed (pending + overdue).
- Monthly collection progress bar showing collected vs expected.
- Quick actions grouped into:
  - **Notices**: Announcements, Leave Notices
  - **Records**: Payments, Attendance
  - **People & Safety**: Boarders, Security
  - **Emergency**: Emergency Contacts
- Tap your avatar to open the **Guardian Account** modal (name, username, house, boarder count, monthly collected, receipts to review, present today, sign out).

### Payments

- View all boarders' payment records for the current period.
- Configure **Billing Settings**: due day, grace period, penalty type/amount, reminder schedule (before 3/1 day, on day, after 1/3 days), and channels (in-app, SMS, email).
- Guardian reviews boarder-submitted receipts:
  - **Pending** receipts can be verified (counts toward balance) or rejected (adjusts outstanding balance).
  - A payment counts as paid only after guardian verification.
- Record payments as `Cash`, `GCash`, `Bank Transfer`, or `Online`.

### Attendance

- View attendance sessions (morning/evening) generated from the worship schedule.
- Check boarders in per session using QR or manual method.
- Sessions enforce a check-in window and QR validity deadline; scans outside the window are rejected.
- Send **absence alerts** to guardians of boarders who miss sessions beyond the threshold.

### Security

- Manage **boarder accounts**: create boarder records, set ID numbers and passwords.
- Log **incidents** (missed curfew, SOS, missed worship, maintenance, other).
- File **maintenance reports** (plumbing, electrical, furniture, appliance, other).
- Manage **rooms** (name, floor, max capacity) and **jobs**.
- View **guardian profile** and emergency contacts.
- Deep-link from dashboard to boarder payments or attendance.

### More

- Save/edit **rooms** and **jobs**.
- Configure **billing settings** and **worship schedule**.
- **Sign out**.

---

## 3. Boarder — role walkthrough

The Boarder shell mirrors the guardian experience from the boarder's perspective:

- View announcements addressed to you (or all boarders).
- Submit **payments** for the current period, optionally attaching a receipt image (saved to app storage; only the path is shared during sync).
- **Check in** to attendance sessions using your static QR code within the valid window.
- Submit **leave notices** with reason, destination, dates, and notes. Track status: Pending → Approved/Rejected → Departed → Completed (Arrived).
- View your profile and guardian/house information.

---

## 4. Shared: Wireless Sync

Both roles can configure synchronization:

1. Open **Wireless Sync**.
2. Enter the computer's IPv4 address running the MBHM sync server (e.g. `http://192.168.1.42:3000`).
3. Paste the server **access token** printed when the server started.
4. Tap **Save Address** or **Sync Now**.

The app stores the token encrypted using Android Keystore and syncs automatically every 30 seconds when enabled.

### Phone-to-phone sync

- The guardian phone can **Start Sharing**, generating an IP address and a temporary **pairing token**.
- Other devices enter the host IP and token, then tap **Sync With Device**.
- Boarders can join a host but cannot host.
- Tap **Stop Sharing** when done; this invalidates the pairing token.

> ⚠️ Sync uses plain HTTP on the local network. Use a trusted private Wi-Fi only. See [SECURITY.md](SECURITY.md).

---

## 5. Statuses you'll see

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

## 6. Important limitations

- This is a **student/demo build**, not a production deployment.
- Sync traffic is **not encrypted** (plain HTTP). Do not use on public Wi-Fi.
- The sync token is **not per-user authorization** — all authenticated clients see shared collections.
- The boarder QR is a **static account identifier**, not live-presence proof.
- Receipt image files stay on the submitting device; only the path is synced.
- SOS incidents are recorded and shared on sync, but there is **no live SMS/push alert**.
- Use unique, non-demo data while developing.

---

## Related docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — how the app is built
- [SYNC.md](SYNC.md) — synchronization details
- [SECURITY.md](SECURITY.md) — security limitations and hardening
- [GETTING_STARTED.md](GETTING_STARTED.md) — building and running the app