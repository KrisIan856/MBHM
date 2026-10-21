# Boarder Dashboard & Screen Reference — MBHM

This document describes every screen a **Boarder** can see and use in MBHM, tab by tab. It complements [USER_GUIDE.md](USER_GUIDE.md) with screen-level detail.

The Boarder shell (`boarder/BoarderApp.kt`) has a five-item bottom navigation:

| Tab | Icon | Screen |
|-----|------|--------|
| Home | 🏠 | BHome |
| Payments | 💳 | BPayments |
| Attendance | 📅 | BAttendance |
| Security | 🛡️ | BSecurity |
| More | •• | BMore |

A sixth screen, **Announcements** (`BAnnouncements`), opens from the Home quick actions. Overlays include the SOS confirmation modal, contact edit modal, leave notice form/review modal, and QR preview.

> If the boarder account is not linked (`boarderId == null`), the app shows "Account not linked" instead of the dashboard.

---

## 1. Home (`BHome`)

The Boarder home is a personal dashboard with greetings, announcements, quick access, attendance QR, rent status, and SOS.

### Header
- Time-based greeting: "Good morning / Good afternoon / Good evening, <FirstName>! 👋"
- "Boarder" role chip
- "Room <n> · Casa Marigold"
- Avatar with boarder initials and avatar color
- Unread announcement count badge (red dot on avatar)

### Today's Announcement
- Shows up to 3 announcements addressed to this boarder (or all)
- Each card: icon (📢 normal / 🔴 critical), title, body, sent at, "New" / "✓ Read" / "✓ Ack" indicator
- "See all →" opens BAnnouncements
- If none: "No announcement yet — Your guardian posts updates here"

### Quick Access (six tiles)
| Tile | Icon | Action |
|------|------|--------|
| Attendance QR | 📱 | Opens attendance screen, QR view |
| Payments | 💳 | Opens Payments screen |
| My History | 📈 | Opens attendance history |
| Maintenance | 🔧 | Opens Security → Maintenance |
| Security | 🛡️ | Opens Security & Emergency overview |
| Emergency | 📞 | Opens emergency contact |

### My Attendance QR (expandable card)
- QR code ("boarder-<id>") with label "My Attendance QR"
- Subtitle: "Show this to your guardian to check in"
- "Tap to enlarge →" opens full QR view
- QR is accepted only during an active check-in window

### Monthly Rent Card
- Period label (e.g. "September")
- "Monthly Rent" and amount due (large, e.g. "₱3,000")
- Status:
  - If fully paid: "✓ Paid" chip
  - If not fully paid: "Due — <Day>th of month" and "Upload Receipt →" shortcut (opens Payments)
- Background color reflects status: green (paid), blue (partial), red (overdue/pending)

### Emergency SOS (red card)
- "🆘 Emergency SOS" — "Tap to alert guardian immediately"
- Opens a confirmation modal: "This will immediately alert the house guardian of an emergency. Only use in genuine emergency situations."
- Confirm sends an SOS incident (shared with guardian on next sync; not an instant SMS/push)
- Toast: "🆘 SOS alert sent! Guardian has been notified."

---

## 2. Payments (`BPayments`)

Boarder views rent status, creates payments, and uploads proof of payment.

### Period Picker
- PayPeriodPager showing all months with payment records
- Boarder selects a month to view its account

### Payment Status Banner (top card)
- Period name
- Status badge:
  - ⏳ PENDING VERIFICATION (receipt awaiting guardian review)
  - ✓ FULLY PAID (green)
  - ½ PARTIALLY PAID (blue)
  - ⚠ OVERDUE (red)
  - ⏳ UNPAID
- Amount due (large)
- Paid date, Amount paid, Remaining balance

### Payment Reminder (when not fully paid)
- Warning card: "Rent for <month> is due on day <N> · <balance> outstanding"

### Account Breakdown
- Monthly Rent (billed)
- Amount Paid (if > 0)
- Balance Due / Fully Paid

### Proof of Payment
If a receipt exists for the month:
- Receipt file name and ReceiptChip status (Pending review / Verified / Rejected)
- Receipt image preview (120 dp height; image stored on this device only)
- If REJECTED: "Proof Rejected by Guardian" with reason and "Upload New Proof" button
- If VERIFIED: "Verified <date> by Guardian"
If no receipt:
- "Upload your GCash, bank transfer, or payment receipt image (JPG, JPEG, PNG)."
- "Upload Proof of Payment" button opens file picker

### All My Months (history table)
Table of past months: Period | Billed | Paid | Balance, with PayChip status. Tapping a month switches the view.

### Actions
- "＋ New Payment" / "＋ Record Top-up": opens New Payment modal
  - Period, amount (₱), payment method (GCash / Cash / Bank)
  - Warning if month already fully paid
  - Creates a payment record and prompts to upload proof
- "History": opens BPay_History

### Payment History (`BPay_History`)
- Date range filter + status filter (All / Paid / Pending / Partial / Overdue)
- Stats: months in range, total paid, outstanding
- List of payments: period, status chip, billed / paid / balance, paid date · method, receipt chip, verified info

---

## 3. Attendance (`BAttendance`)

Boarder views check-in status, shows QR to guardian, and reviews history.

### Today — <date>
Two cards side by side: Morning (☀️) and Evening (🌙).

Each card shows:
- Session window (e.g. "6:00 AM – 7:00 AM")
- If PRESENT: "✅ <check-in time>" (green)
- If ABSENT: "❌ Absent" + "Not checked in by <window end>" (red)
- If not yet checked in and window is open: "Show My QR" button
- If window not started / closed: scheduled time or "Check-in closed"

### QR Code Display (full screen)
- Session: "☀️ Morning Worship Check-in" or "🌙 Evening Worship Check-in" with window time
- QR live status:
  - "● Check-in open · closes at <time>" (green)
  - "● Check-in opens at <time>" (muted, not started)
  - "✕ Check-in closed at <time>" (red)
- Large QR code (seed: boarder-<id>)
- Boarder name, Room, ID
- How to use: 1) Show QR to guardian, 2) Guardian scans it, 3) Check-in recorded immediately
- "This QR is unique to your account. Do not share it with other boarders."
- Check-in reminder banner when a window is open

### Monthly Calendar
- Month label and day-of-week headers
- Calendar grid: present days (green), absent days (red), today (blue outline), future days (muted)
- Legend: Present / Absent / Today

### Stats
- Present count, Absent count, Streak

### Absence Threshold Banner (when over limit)
- "⚠ Absence Threshold Reached" — shows missed count vs limit
- Whether guardian was notified and how many newer misses are pending review

### Attendance History (`BAttn_History`)
- Date range filter + session filter (All / Morning / Evening)
- Stats: records, attendance rate %, present, absent
- List: date, session type (☀️ Morning / 🌙 Evening), check-in time, method (📱 QR Scan / ✏️ Manual Check), override reason, Present / Absent chip

---

## 4. Security & Emergency (`BSecurity`)

Boarder's safety and contact hub.

### Emergency SOS
- Red card: "🆘 Emergency SOS — Tap to send instant alert to guardian"
- Confirmation modal explains: records an SOS incident, shared on next sync, not an immediate message
- Confirm creates an Incident (type SOS) and shows toast: "🆘 SOS recorded — it will be shared on the next successful sync"

### Emergency Contact
- Shows emergency contact name and phone (callable)
- "✏ Edit" opens form to edit name and phone
- "⚠ Emergency Contact Info" warning: "This person will be contacted in case of emergency. Keep it updated."

### House Guardian
- Shows guardian contact phone (callable) or "No guardian contact number saved yet"

### Maintenance
- Count of this boarder's maintenance requests
- "+ New" to submit a maintenance request
- List of reports: category emoji + label (Plumbing 🚿, Electrical 💡, Furniture 🛋️, Appliance ❄️, Other 🔧), status chip (Open / In Progress / Resolved), description, submitted at, resolved at
- Submit form: category, description, room (pre-filled), status = Open

---

## 5. More (`BMore`)

Boarder settings, profile, leave notices, sync, and sign out.

### Profile header tile
- Avatar, name, "Room <n> · <job>", phone
- "View →" opens My Profile

### Menu items
| Item | Icon | Opens |
|------|------|-------|
| Leave Notice | 📅 | Leave Notice form + history |
| Profile & Contacts | 👤 | My Profile |
| Wireless Sync | 🔄 | DeviceSyncScreen (view-only; boarders cannot host) |
| Settings | ⚙️ | Preferences |
| Sign Out | (red) | Confirmation dialog, then signs out |

### My Profile
- Avatar, name, job
- Room & Tenancy: Room, Floor, Move-in Date, Monthly Rate, Due Day, Grace Period
- Contact Information: Phone, Email, House Guardian (with ✏ Edit)
- Emergency Contact: Name, number, secondary, address (with ✓ Edit)
- Guardian Contact: Name, number, alternative, address (with ✓ Edit)
- Edit Contact Information modal: pre-filled form for emergency + guardian contacts, validation, Save Contacts

### Leave Notices
- "+ File New Leave Notice" button opens two-step modal:
  1. **Form**: Leave Date, Expected Departure Time, Expected Return Date, Expected Return Time, Reason, Destination, Additional Notes (optional)
  2. **Review**: summary of all fields, then "Submit Leave Notice"
- Statuses: Pending, Approved, Rejected, Out now (Departed), Arrived (Completed)
- Each notice card: reason, status chip, destination, departure · expected return, notes, submitted at, timeline (actually departed / actually arrived)
- If APPROVED: "🚪 I'm Leaving Now" button → marks DEPARTED
- If DEPARTED: "Out now — waiting for the guardian to confirm your arrival"
- If COMPLETED: "Trip complete — your arrival was confirmed by the guardian"
- List history: all filed notices with EmptyState if none

### Wireless Sync (view-only for boarders)
- Boarders can join a host but **cannot host** the shared database (`allowHosting = false`)
- Enter host IP + pairing token, tap Sync With Device

### Settings
Preference toggles (saved on device):
- **Attendance Settings**: Session Reminders, Absence Alerts, Calendar Present/Absent Colors
- **Payment Settings**: Payment Due Reminders, Upload Prompt on Home, Payment History Link
- **Notifications**: Announcement Alerts
- **App Info**: Version 1.0.0, Build 2026.02

---

## 6. Announcements (`BAnnouncements`)

Boarder reads announcements addressed to them.

- Stats: Total, Unread, Critical
- Date range filter
- Priority filter: All / 🔴 Critical / 📢 Info
- Read filter: All / Unread / Read
- List cards: icon, title, body, "Posted by <name> · <time>", "New" / "✓ Read" / "✓ Ack" indicator
- Tap a card to open detail
- Detail view: full title, body, posted by/time, "✓ Marked as read"
- For Critical announcements not yet acknowledged: "✓ Acknowledge" button
- After acknowledging: "✓ You have acknowledged this announcement"

---

## 7. Boarder Data Summary (what the boarder sees across all screens)

| Domain | What boarder views | What boarder can edit/create |
|--------|-------------------|-------------------------------|
| Profile | Name, job, room, floor, join date, rate, due day, grace, phone, email | Update profile via contacts edit |
| Payments | Monthly rent, paid amount, balance, receipts, history | Create payments, upload proof, view history |
| Attendance | QR code, today check-in status, calendar, stats, history | Show QR to guardian, view history |
| Announcements | Addressed announcements, read/ack status | Mark read, acknowledge Critical |
| Leave Notices | Filed notices, statuses, timeline | Submit new leave notices, "I'm Leaving Now" |
| Maintenance | Own reports, statuses | Submit new maintenance requests |
| Emergency | SOS button, emergency contact, guardian phone | Send SOS, edit contacts |
| Contacts | Emergency + guardian contact info | Edit both contact sets |
| Sync | Wireless sync (join only) | Enter host IP + token, sync (cannot host) |
| Settings | Notification & preference toggles | Toggle settings on/off |

---

## Related docs

- [USER_GUIDE.md](USER_GUIDE.md) — feature walkthrough for the Boarder role
- [SYNC.md](SYNC.md) — wireless and phone-to-phone synchronization
- [API.md](API.md) — sync server endpoint reference
- [DATABASE.md](DATABASE.md) — Room schema reference
- [SECURITY.md](SECURITY.md) — security limitations and hardening