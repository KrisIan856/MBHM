# Guardian Dashboard & Screen Reference — MBHM

This document describes every screen a **Guardian** can see and use in MBHM, tab by tab. It complements [USER_GUIDE.md](USER_GUIDE.md) with screen-level detail.

The Guardian shell (`guardian/GuardianApp.kt`) has a five-item bottom navigation:

| Tab | Icon | Screen |
|-----|------|--------|
| Dashboard | 🏠 | GDashboard |
| Payments | 💳 | GPayments |
| Attendance | 📅 | GAttendance |
| Security | 🛡️ | GSecurity |
| More | •• | GMore |

A sixth screen, **Announcements** (`GAnnouncements`), opens from the Dashboard quick actions. A **Guardian Account** modal and a **Leave Notices** modal open as overlays from the Dashboard.

---

## 1. Dashboard (`GDashboard`)

The Dashboard is the guardian's home. It shows account overview, monthly collection progress, and grouped quick actions.

### Header
- Time-based greeting: "Good morning / Good afternoon / Good evening, 👋"
- Guardian name and house ("Casa Marigold")
- Guardian role chip
- Circular avatar with guardian initial — tapping it opens the **Guardian Account** modal

### Stat Cards
| Card | Shows |
|------|-------|
| Fully Paid | Count of boarders fully paid this month · "of N boarders" |
| Partially Paid | Count partially paid · "balance still due" |
| Present Today | Count present today · "N absent" |
| Action Needed | Pending + overdue count · "N overdue · N pending" |

### Monthly Collection Panel
- Period label (e.g. "September Collection")
- Collected amount vs total expected (e.g. "₱12,000 of ₱15,000")
- Progress bar split into Paid (green), Partial (blue), and outstanding (empty)
- Summary line: "N fully paid · N partial · N outstanding"

### Quick Actions (grouped)

**Notices**
- Announcements — opens GAnnouncements
- Leave Notices — opens the Leave Notices modal

**Records**
- Payments — switches tab to Payments
- Attendance — switches tab to Attendance

**People & Safety**
- Boarders — switches tab to Security (boarder accounts deep link)
- Security — switches tab to Security

**Emergency**
- Emergency Contacts — deep link to Security contacts

### Guardian Account Modal
Opened from the avatar. Shows:
- Avatar with initial, full name, username ("@username"), house ("Casa Marigold"), role chip ("Guardian")
- Account & Overview card:
  - Full name, Username, House
  - Boarders registered (count)
  - Collected this month (paid amount of total due)
  - Receipts to review (count of PENDING_REVIEW receipts)
  - Present today (count of boarders present)
- Sign Out button (red)

### Leave Notices Modal
Opened from Dashboard. Guardian reviews leave notices submitted by boarders:
- Date range filter (From / To)
- Status chip bar: All, Pending, Approved, Departed, Rejected, Arrived (Completed)
- Each notice shows: reason, leave date · destination, sender name · room, submitted at, sender phone with Call button
- Status chips: Pending (warn), Approved (sage), Rejected (danger), Departed (primary), Arrived (blue)
- Verified by / declined by info with timestamp when verified
- For PENDING notices: Approve and Decline buttons
- For APPROVED: "Waiting for the boarder to tap 'I'm Leaving Now'"
- For DEPARTED: "Mark as Arrived" button (moves to COMPLETED)
- For COMPLETED: "Trip complete — boarder is back"

---

## 2. Payments (`GPayments`)

Guardian views and verifies boarder payments.

### Billing Settings (editable by guardian)
- Due day, grace period days
- Penalty type (Flat / Percentage) and penalty amount
- Reminder toggles: before 3 days, before 1 day, on day, after 1 day, after 3 days
- Channel toggles: in-app, SMS, email

### Payment Records
List of all boarders' payments for the current period, showing:
- Boarder name / room
- Monthly rate (billed)
- Amount paid
- Balance due
- Status: Paid / Partial / Pending / Overdue
- Receipt status: Pending review / Verified / Rejected

### Receipt Verification (guardian action)
For each boarder-submitted payment with a receipt:
- Receipt file name and preview (image stored on the submitting device; only the path is synced)
- Receipt status chip
- If PENDING_REVIEW: guardian can **Verify** (marks PAID and counts toward balance) or **Reject** (with optional reason; rejection changes outstanding balance)
- If VERIFIED: shows "Verified <date> by Guardian"
- If REJECTED: shows rejection reason and "Proof Rejected by Guardian"

### Accounting rule
A boarder-submitted payment counts toward the balance **only after the guardian verifies the receipt**. Pending and rejected proofs no longer look paid.

---

## 3. Attendance (`GAttendance`)

Guardian manages worship/attendance sessions and check-ins.

### Attendance Sessions
- Morning and Evening sessions generated from the Worship Schedule
- Each session shows date, window start–end, and QR validity minutes
- Guardian ensures today's sessions exist (`ensureTodaySessions`)

### Check-In Records
- Per boarder, per session: status (Present / Absent), check-in time, method (QR / Manual)
- Guardian can record manual check-ins and overrides

### Absence Alerts
- Guardian configures the absence threshold (in Worship Schedule)
- When a boarder misses sessions beyond the threshold, an absence alert is created
- Guardian can send / upsert absence alerts to notify themselves

### Worship Schedule (editable)
- Morning start / end and QR validity minutes
- Evening start / end and QR validity minutes
- Absence threshold (number of misses before alert)

### Check-in window enforcement
- Scans outside the session start / QR-validity window are rejected
- The static boarder QR is not accepted before or after the window

---

## 4. Security (`GSecurity`)

Guardian manages boarder accounts, incidents, maintenance, rooms, jobs, and contacts.

### Boarder Accounts
- Create boarder account: name, ID number, room, floor, job, monthly rate, password
- Guardian issues the ID number and creates the password
- List of all boarders with name, room, floor, initials, avatar color, phone, email, join date, monthly rate, payment status

### Incidents
- Log incidents: Missed Curfew, SOS, Missed Worship, Maintenance, Other
- Each incident: type, boarder, title, description, timestamp, resolved flag
- Guardian can mark incidents resolved

### Maintenance Reports
- File maintenance tickets: boarder, room, category (Plumbing / Electrical / Furniture / Appliance / Other), description, status (Open / In Progress / Resolved)
- Track resolution with resolved-at timestamp

### Rooms & Jobs
- Save room: name, floor, max capacity
- Delete room
- Save job / occupation name
- Delete job

### Guardian Profile
- Name, phone, email, house name, house address (shared with all boarders)

### Emergency Contacts
- Guardian contact phone (callable from boarder screens)

---

## 5. More (`GMore`)

Settings and house configuration.

- **Rooms**: add / delete rooms (name, floor, max capacity)
- **Jobs**: add / delete job names
- **Billing Settings**: due day, grace period, penalty, reminders, channels
- **Worship Schedule**: morning/evening times, QR validity, absence threshold
- **Sign Out**: clears session and returns to sign-in

---

## 6. Announcements (`GAnnouncements`)

Guardian posts announcements visible to boarders.

- Create announcement: title, body, priority (Normal / Critical), recipients (specific boarders or all = null)
- List shows all announcements with sent at, sent by, read by, acknowledged by
- Guardian can upsert announcements
- Boarders can mark announcements read and acknowledge Critical ones

---

## 7. Guardian Data Summary (what the guardian sees across all screens)

| Domain | What guardian views | What guardian can edit/create |
|--------|-------------------|-------------------------------|
| Boarders | Full roster with contacts, room, rate, payment status | Create accounts, set ID/password, update boarder info |
| Payments | All boarders' monthly payments, receipts, balances | Configure billing, verify/reject receipts, record payments |
| Attendance | Sessions, check-in records, present/absent counts | Record manual check-ins, configure schedule & threshold |
| Announcements | All announcements, read/ack status | Create and send announcements |
| Leave Notices | All boarder leave requests with status | Approve / decline / mark departed / mark arrived |
| Incidents | All incidents (SOS, curfew, etc.) | Log and resolve incidents |
| Maintenance | All maintenance reports | File and resolve reports |
| Rooms / Jobs | Room roster, job list | Add / delete rooms and jobs |
| Guardian Profile | House profile | Edit house name, address, contacts |
| Sync | (via Wireless Sync screen) | Configure server host & token, trigger sync |

---

## Related docs

- [USER_GUIDE.md](USER_GUIDE.md) — feature walkthrough for the Guardian role
- [SYNC.md](SYNC.md) — wireless and phone-to-phone synchronization
- [API.md](API.md) — sync server endpoint reference
- [DATABASE.md](DATABASE.md) — Room schema reference
- [SECURITY.md](SECURITY.md) — security limitations and hardening