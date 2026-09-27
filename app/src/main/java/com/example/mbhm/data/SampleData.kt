package com.example.mbhm.data

import com.example.mbhm.model.*

object SampleData {

    // ─── Boarders ──────────────────────────────────────────────────────────────

    val boarders: List<Boarder> = listOf(
        Boarder("1", "Maria Santos", "101", "1F", "MS", "#D4C8E8", "09171234567", "maria@email.com", "Jan 15, 2024", 4500, 10, 3, PayStatus.PAID, PenaltyType.FLAT, 200,
            EmergencyContact("Juan Santos", "Father", "09170001111", "09170001112", "Block 4 Lot 12, Guadalupe, Cebu City"),
            GuardianContact("Sandra Santos", "Mother / House Guardian", "09179998888", "09178887777", "Casa Marigold, Cebu City"),
            "Nurse"
        ),
        Boarder("2", "Carlo Reyes", "102", "1F", "CR", "#B8D4C0", "09281234567", "carlo@email.com", "Feb 1, 2024", 4500, 10, 3, PayStatus.PAID, PenaltyType.FLAT, 200,
            EmergencyContact("Ana Reyes", "Mother", "09280002222", null, "Lahug, Cebu City"),
            GuardianContact("Sandra Santos", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Engineer"
        ),
        Boarder("3", "Lena Cruz", "103", "1F", "LC", "#F0C8C8", "09391234567", "lena@email.com", "Mar 10, 2024", 5000, 10, 3, PayStatus.OVERDUE, PenaltyType.FLAT, 200,
            EmergencyContact("Pedro Cruz", "Father", "09390003333", "09390003334", "Mandaue City"),
            GuardianContact("Sandra Santos", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Teacher"
        ),
        Boarder("4", "Diego Flores", "201", "2F", "DF", "#C8D8F0", "09171112233", "diego@email.com", "Nov 20, 2023", 5000, 10, 3, PayStatus.PARTIAL, PenaltyType.FLAT, 200,
            EmergencyContact("Rosa Flores", "Mother", "09170004444", "09170004445", "Talisay City"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", "09178887777", "Casa Marigold, Cebu City"),
            "Student"
        ),
        Boarder("5", "Rina Bautista", "202", "2F", "RB", "#F0DCC8", "09284445566", "rina@email.com", "Apr 5, 2024", 4500, 10, 3, PayStatus.PAID, PenaltyType.PERCENTAGE, 5,
            EmergencyContact("Ben Bautista", "Father", "09280005555", null, "Lapu-Lapu City"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Call Center"
        ),
        Boarder("6", "Marco Villanueva", "203", "2F", "MV", "#D8C8F0", "09397778899", "marco@email.com", "Jan 8, 2024", 5500, 10, 3, PayStatus.PAID, PenaltyType.FLAT, 200,
            EmergencyContact("Lisa Villanueva", "Mother", "09390006666", null, "Banilad, Cebu City"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Architect"
        ),
        Boarder("7", "Sofia Mendoza", "301", "3F", "SM", "#C8F0D8", "09171002200", "sofia@email.com", "May 15, 2024", 5500, 10, 3, PayStatus.PARTIAL, PenaltyType.FLAT, 200,
            EmergencyContact("Carlos Mendoza", "Father", "09170007777", null, "Consolacion"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Nurse"
        ),
        Boarder("8", "Jake Aquino", "302", "3F", "JA", "#F0ECC8", "09283003300", "jake@email.com", "Jun 1, 2024", 5000, 10, 3, PayStatus.OVERDUE, PenaltyType.FLAT, 200,
            EmergencyContact("May Aquino", "Mother", "09280008888", null, "Liloan"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Driver"
        ),
        Boarder("9", "Tricia Lim", "303", "3F", "TL", "#C8F0EC", "09394004400", "tricia@email.com", "Sep 12, 2023", 4500, 10, 3, PayStatus.PAID, PenaltyType.FLAT, 200,
            EmergencyContact("James Lim", "Father", "09390009999", null, "Cebu City"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Accountant"
        ),
        Boarder("10", "Ramon Dela Cruz", "401", "4F", "RD", "#E8C8F0", "09175005500", "ramon@email.com", "Jul 20, 2024", 6000, 10, 3, PayStatus.PAID, PenaltyType.PERCENTAGE, 5,
            EmergencyContact("Grace Dela Cruz", "Spouse", "09170010000", null, "Cebu City"),
            GuardianContact("Ate Sandra", "House Guardian", "09179998888", null, "Casa Marigold, Cebu City"),
            "Manager"
        )
    )

    val loggedInBoarder: Boarder = boarders[3] // Diego Flores — partial

    // ─── Leave Notices ─────────────────────────────────────────────────────────

    val leaveNotices: List<LeaveNotice> = listOf(
        LeaveNotice("ln1", "4", "Sep 15, 2026", "08:00 AM", "Sep 18, 2026", "06:00 PM", "Family Gathering", "Davao City", "Will stay with relatives", LeaveNoticeStatus.APPROVED, "Sep 10, 2026"),
        LeaveNotice("ln2", "4", "Oct 01, 2026", "07:00 AM", "Oct 03, 2026", "08:00 PM", "Medical Checkup", "Cebu Doctors Hospital", "Scheduled appointment", LeaveNoticeStatus.PENDING, "Sep 12, 2026"),
        LeaveNotice("ln3", "1", "Sep 20, 2026", "09:00 AM", "Sep 22, 2026", "05:00 PM", "Home Visit", "Bacolod City", null, LeaveNoticeStatus.PENDING, "Sep 11, 2026")
    )

    // ─── Payment Records ───────────────────────────────────────────────────────

    val payments: List<PaymentRecord> = listOf(
        // Sep 2026
        PaymentRecord("p1", "1", "Sep 2026", 4500, "Sep 3", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_sep3.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Sep 3 · 2:10 PM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p2", "2", "Sep 2026", 4500, "Sep 1", PayMethod.BANK_TRANSFER, PayStatus.PAID, ReceiptStatus.VERIFIED, "bdo_sep1.pdf", recordedBy = RecordedBy.BOARDER, verifiedAt = "Sep 1 · 4:30 PM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p3", "3", "Sep 2026", 5000, null, PayMethod.CASH, PayStatus.OVERDUE, recordedBy = RecordedBy.BOARDER),
        PaymentRecord("p4", "4", "Sep 2026", 5000, "Sep 6", PayMethod.GCASH, PayStatus.PARTIAL, ReceiptStatus.PENDING_REVIEW, "gcash_sep7.jpg", amountPaid = 2500, recordedBy = RecordedBy.BOARDER, notes = "Partial — balance ₱2,500 to follow"),
        PaymentRecord("p5", "5", "Sep 2026", 4500, "Sep 2", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_sep2.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Sep 2 · 11:00 AM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p6", "6", "Sep 2026", 5500, "Sep 4", PayMethod.CASH, PayStatus.PAID, recordedBy = RecordedBy.GUARDIAN, verifiedBy = "Ate Sandra", notes = "Cash — received in person"),
        PaymentRecord("p7", "7", "Sep 2026", 5500, "Sep 5", PayMethod.CASH, PayStatus.PARTIAL, amountPaid = 3000, recordedBy = RecordedBy.GUARDIAN, notes = "Partial cash — balance ₱2,500 to follow"),
        PaymentRecord("p8", "8", "Sep 2026", 5000, null, PayMethod.CASH, PayStatus.OVERDUE, recordedBy = RecordedBy.BOARDER),
        PaymentRecord("p9", "9", "Sep 2026", 4500, "Sep 1", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_tricia.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Sep 1 · 9:45 AM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p10", "10", "Sep 2026", 6000, "Sep 5", PayMethod.BANK_TRANSFER, PayStatus.PAID, ReceiptStatus.REJECTED, "bdo_sep5.pdf", rejectionReason = "Amount mismatch — shows ₱5,500 not ₱6,000", recordedBy = RecordedBy.BOARDER, verifiedAt = "Sep 5 · 3:00 PM", verifiedBy = "Ate Sandra"),
        // Aug 2026
        PaymentRecord("p11", "1", "Aug 2026", 4500, "Aug 4", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_aug4.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Aug 4 · 1:00 PM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p12", "2", "Aug 2026", 4500, "Aug 3", PayMethod.BANK_TRANSFER, PayStatus.PAID, ReceiptStatus.VERIFIED, "bdo_aug3.pdf", recordedBy = RecordedBy.BOARDER, verifiedAt = "Aug 3 · 5:00 PM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p13", "3", "Aug 2026", 5000, "Aug 12", PayMethod.CASH, PayStatus.PAID, recordedBy = RecordedBy.GUARDIAN, notes = "Cash — late, penalty ₱200 applied"),
        PaymentRecord("p14", "4", "Aug 2026", 5000, "Aug 8", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_aug8.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Aug 8 · 10:00 AM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p15", "5", "Aug 2026", 4500, "Aug 2", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_aug2.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Aug 2 · 2:00 PM", verifiedBy = "Ate Sandra"),
        // Jul 2026
        PaymentRecord("p16", "4", "Jul 2026", 5000, "Jul 9", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_jul9.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Jul 9 · 3:30 PM", verifiedBy = "Ate Sandra"),
        PaymentRecord("p17", "3", "Jul 2026", 5000, "Jul 7", PayMethod.GCASH, PayStatus.PAID, ReceiptStatus.VERIFIED, "gcash_jul7.jpg", recordedBy = RecordedBy.BOARDER, verifiedAt = "Jul 7 · 11:00 AM", verifiedBy = "Ate Sandra"),
    )

    // ─── Attendance ────────────────────────────────────────────────────────────

    val worshipSchedule = WorshipSchedule(
        morningStart = "06:00", morningEnd = "08:00", morningQRMins = 15,
        eveningStart = "20:00", eveningEnd = "22:00", eveningQRMins = 15,
        absenceThreshold = 3
    )

    val sessions: List<AttendanceSession> = listOf(
        AttendanceSession("s1", SessionType.MORNING, "Sep 7, 2026", "6:00 AM", "8:00 AM", 15),
        AttendanceSession("s2", SessionType.EVENING, "Sep 7, 2026", "8:00 PM", "10:00 PM", 15),
        AttendanceSession("s3", SessionType.MORNING, "Sep 6, 2026", "6:00 AM", "8:00 AM", 15),
        AttendanceSession("s4", SessionType.EVENING, "Sep 6, 2026", "8:00 PM", "10:00 PM", 15),
        AttendanceSession("s5", SessionType.MORNING, "Sep 5, 2026", "6:00 AM", "8:00 AM", 15),
        AttendanceSession("s6", SessionType.EVENING, "Sep 5, 2026", "8:00 PM", "10:00 PM", 15),
        AttendanceSession("s7", SessionType.MORNING, "Sep 4, 2026", "6:00 AM", "8:00 AM", 15),
        AttendanceSession("s8", SessionType.EVENING, "Sep 4, 2026", "8:00 PM", "10:00 PM", 15),
    )

    val attendanceRecords: List<AttendanceRecord> = listOf(
        AttendanceRecord("a1", "1", "s1", AttendanceStatus.PRESENT, "6:14 AM", AttendanceMethod.QR),
        AttendanceRecord("a2", "2", "s1", AttendanceStatus.PRESENT, "6:22 AM", AttendanceMethod.QR),
        AttendanceRecord("a3", "3", "s1", AttendanceStatus.ABSENT, null, AttendanceMethod.MANUAL, "Did not check in — window closed", "Ate Sandra"),
        AttendanceRecord("a4", "4", "s1", AttendanceStatus.PRESENT, "6:55 AM", AttendanceMethod.QR),
        AttendanceRecord("a5", "5", "s1", AttendanceStatus.PRESENT, "6:08 AM", AttendanceMethod.QR),
        AttendanceRecord("a6", "6", "s1", AttendanceStatus.ABSENT, null, AttendanceMethod.MANUAL, "Informed guardian of medical leave", "Ate Sandra"),
        AttendanceRecord("a7", "7", "s1", AttendanceStatus.PRESENT, "7:10 AM", AttendanceMethod.QR),
        AttendanceRecord("a8", "8", "s1", AttendanceStatus.PRESENT, "6:45 AM", AttendanceMethod.QR),
        AttendanceRecord("a9", "9", "s1", AttendanceStatus.PRESENT, "6:18 AM", AttendanceMethod.QR),
        AttendanceRecord("a10", "10", "s1", AttendanceStatus.ABSENT, null, AttendanceMethod.MANUAL, "Night shift — excused", "Ate Sandra"),
        AttendanceRecord("a11", "1", "s3", AttendanceStatus.PRESENT, "6:10 AM", AttendanceMethod.QR),
        AttendanceRecord("a12", "2", "s3", AttendanceStatus.PRESENT, "6:18 AM", AttendanceMethod.QR),
        AttendanceRecord("a13", "3", "s3", AttendanceStatus.ABSENT, null, AttendanceMethod.MANUAL, "Auto-flagged", "System"),
        AttendanceRecord("a14", "4", "s3", AttendanceStatus.ABSENT, null, AttendanceMethod.MANUAL, "Auto-flagged", "System"),
        AttendanceRecord("a15", "5", "s3", AttendanceStatus.PRESENT, "6:05 AM", AttendanceMethod.QR),
        AttendanceRecord("a16", "6", "s3", AttendanceStatus.PRESENT, "6:42 AM", AttendanceMethod.QR),
        AttendanceRecord("a17", "7", "s3", AttendanceStatus.PRESENT, "7:01 AM", AttendanceMethod.QR),
        AttendanceRecord("a18", "8", "s3", AttendanceStatus.PRESENT, "6:28 AM", AttendanceMethod.QR),
        AttendanceRecord("a19", "9", "s3", AttendanceStatus.PRESENT, "6:12 AM", AttendanceMethod.QR),
        AttendanceRecord("a20", "10", "s3", AttendanceStatus.ABSENT, null, AttendanceMethod.MANUAL, "Night shift — excused", "Ate Sandra"),
    )

    // ─── Announcements ─────────────────────────────────────────────────────────

    val announcements: List<Announcement> = listOf(
        Announcement(
            "ann1",
            "Water shut-off — Sep 9",
            "The water supply will be turned off on September 9, 2026 from 8:00 AM to 10:00 AM for scheduled maintenance. Please store water in advance.",
            Priority.NORMAL,
            null,
            "Sep 6, 2:00 PM",
            "Ate Sandra",
            listOf("1", "2", "4", "5", "9"),
            emptyList()
        ),
        Announcement(
            "ann2",
            "Monthly room inspection — Sep 15",
            "Your rooms will be inspected on September 15, 2026. Please ensure rooms are clean and all personal belongings are properly stored. Inspection starts at 9:00 AM.",
            Priority.CRITICAL,
            null,
            "Sep 5, 10:00 AM",
            "Ate Sandra",
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
            listOf("1", "2", "5", "9", "10")
        ),
        Announcement(
            "ann3",
            "Wifi password changed",
            "The boarding house wifi password has been updated. The new password is posted on the main bulletin board near the entrance. Please do not share with non-boarders.",
            Priority.NORMAL,
            null,
            "Sep 3, 9:00 AM",
            "Ate Sandra",
            listOf("1", "2", "3", "5", "6", "9"),
            emptyList()
        ),
        Announcement(
            "ann4",
            "Curfew reminder — 10:00 PM",
            "Please be reminded that house curfew is strictly 10:00 PM every night. Boarders arriving after curfew must notify the guardian in advance. Repeated violations will be subject to penalties per house rules.",
            Priority.CRITICAL,
            listOf("3", "4", "8"),
            "Sep 4, 7:00 PM",
            "Ate Sandra",
            listOf("4"),
            emptyList()
        ),
    )

    // ─── Visitor Log ───────────────────────────────────────────────────────────

    val visitorEntries: List<VisitorEntry> = listOf(
        VisitorEntry("v1", "Jessa Santos", "1", "Personal visit", "10:30 AM", "1:00 PM", "Sep 7, 2026"),
        VisitorEntry("v2", "Mr. Reyes Sr.", "2", "Family visit", "2:15 PM", null, "Sep 7, 2026"),
        VisitorEntry("v3", "Delivery Rider", "7", "Package pickup", "3:40 PM", "3:45 PM", "Sep 7, 2026"),
        VisitorEntry("v4", "Ben Cruz", "3", "Personal visit", "11:00 AM", "12:30 PM", "Sep 6, 2026"),
        VisitorEntry("v5", "Alma Flores", "4", "Family visit", "3:00 PM", "5:30 PM", "Sep 5, 2026"),
    )

    // ─── Incidents ─────────────────────────────────────────────────────────────

    val incidents: List<Incident> = listOf(
        Incident("i1", IncidentType.MISSED_CURFEW, "3", "Missed Curfew — Lena Cruz", "Boarded arrived at 11:42 PM. No prior notification.", "Sep 6 · 11:42 PM", false),
        Incident("i2", IncidentType.SOS, "8", "SOS Alert — Jake Aquino", "Boarder triggered emergency alert. Responded: turned out to be accidental.", "Sep 5 · 9:15 PM", true),
        Incident("i3", IncidentType.MISSED_WORSHIP, "4", "3 Missed Worships — Diego Flores", "Diego has missed 3 consecutive worship sessions. Auto-alert sent.", "Sep 5 · 9:00 AM", false),
        Incident("i4", IncidentType.MAINTENANCE, "6", "A/C Malfunction — Room 203", "Marco reported that the air conditioning unit is not cooling. Technician scheduled.", "Sep 4 · 7:30 PM", false),
        Incident("i5", IncidentType.MISSED_CURFEW, "8", "Missed Curfew — Jake Aquino", "Boarder arrived at 10:45 PM. Second offense this month.", "Sep 3 · 10:45 PM", true),
        Incident("i6", IncidentType.OTHER, null, "Common Area — Light Fixture", "Hallway light on 3rd floor flickering. Maintenance requested.", "Sep 2 · 6:00 PM", true),
    )

    // ─── Maintenance Reports ───────────────────────────────────────────────────

    val maintenanceReports: List<MaintenanceReport> = listOf(
        MaintenanceReport("m1", "6", "203", MaintCategory.APPLIANCE, "Air conditioning unit not cooling. Room temperature stays warm even at max settings.", MaintStatus.IN_PROGRESS, "Sep 4, 7:30 PM"),
        MaintenanceReport("m2", "3", "103", MaintCategory.PLUMBING, "Bathroom faucet leaking slowly. Water dripping continuously from the handle.", MaintStatus.OPEN, "Sep 5, 8:00 AM"),
        MaintenanceReport("m3", "8", "302", MaintCategory.FURNITURE, "Door lock is loose and does not latch properly. Requires tightening or replacement.", MaintStatus.RESOLVED, "Aug 28, 10:00 AM", "Aug 30, 2:00 PM"),
        MaintenanceReport("m4", "10", "401", MaintCategory.ELECTRICAL, "Main light fixture flickers every few seconds. Possibly a loose bulb or wiring issue.", MaintStatus.OPEN, "Sep 2, 6:00 PM"),
    )

    // ─── Curfew Records ────────────────────────────────────────────────────────

    val curfewRecords: List<CurfewRecord> = listOf(
        CurfewRecord("c1", "1", "Sep 7, 2026", "10:00 PM", "9:15 PM", CurfewStatus.COMPLIANT),
        CurfewRecord("c2", "2", "Sep 7, 2026", "10:00 PM", "8:30 PM", CurfewStatus.COMPLIANT),
        CurfewRecord("c3", "3", "Sep 7, 2026", "10:00 PM", null, CurfewStatus.PENDING),
        CurfewRecord("c4", "4", "Sep 7, 2026", "10:00 PM", "9:50 PM", CurfewStatus.COMPLIANT),
        CurfewRecord("c5", "5", "Sep 7, 2026", "10:00 PM", "7:00 PM", CurfewStatus.COMPLIANT),
        CurfewRecord("c6", "6", "Sep 7, 2026", "10:00 PM", "9:00 PM", CurfewStatus.COMPLIANT),
        CurfewRecord("c7", "7", "Sep 7, 2026", "10:00 PM", null, CurfewStatus.PENDING),
        CurfewRecord("c8", "8", "Sep 7, 2026", "10:00 PM", "10:48 PM", CurfewStatus.LATE),
        CurfewRecord("c9", "9", "Sep 7, 2026", "10:00 PM", "8:45 PM", CurfewStatus.COMPLIANT),
        CurfewRecord("c10", "10", "Sep 7, 2026", "10:00 PM", null, CurfewStatus.ABSENT),
    )

    // ─── Default Settings ──────────────────────────────────────────────────────

    val defaultBilling = BillingSettings(
        dueDay = 10,
        gracePeriodDays = 3,
        penaltyType = PenaltyType.FLAT,
        penaltyAmount = 200
    )
}
