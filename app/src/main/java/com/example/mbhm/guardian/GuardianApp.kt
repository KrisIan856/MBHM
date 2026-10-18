package com.example.mbhm.guardian

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun GuardianApp(user: com.example.mbhm.data.entity.UserEntity, onLogout: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("dashboard") }

    BackHandler {
        if (tab != "dashboard") {
            tab = "dashboard"
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    val appRepository = remember { AppRepository.getInstance(context) }

    val boarders by appRepository.getAllBoarders().collectAsState(initial = emptyList())
    val payments by appRepository.getAllPayments().collectAsState(initial = emptyList())
    val attnRecords by appRepository.getAllAttendanceRecords().collectAsState(initial = emptyList())
    val announcements by appRepository.getAllAnnouncements().collectAsState(initial = emptyList())
    val incidents by appRepository.getAllIncidents().collectAsState(initial = emptyList())
    val maintenance by appRepository.getAllMaintenanceReports().collectAsState(initial = emptyList())
    val sessions by appRepository.getAllAttendanceSessions().collectAsState(initial = emptyList())
    val leaveNotices by appRepository.observeLeaveNotices().collectAsState(initial = emptyList())
    val billing by appRepository.observeBillingSettings().collectAsState(initial = null)
    val schedule by appRepository.observeWorshipSchedule().collectAsState(initial = null)
    val absenceAlerts by appRepository.getAllAbsenceAlerts().collectAsState(initial = emptyList())
    val rooms by appRepository.observeRooms().collectAsState(initial = emptyList())
    val jobs by appRepository.observeJobs().collectAsState(initial = emptyList())
    val guardianProfile by appRepository.observeGuardianProfile().collectAsState(initial = null)
    var toast by remember { mutableStateOf<String?>(null) }
    var securityDeepLink by remember { mutableStateOf<String?>(null) }
    var paymentsBoarder by remember { mutableStateOf<com.example.mbhm.model.Boarder?>(null) }
    var attendanceBoarder by remember { mutableStateOf<com.example.mbhm.model.Boarder?>(null) }
    var showProfileModal by remember { mutableStateOf(false) }
    var showLeaveNoticesModal by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val today = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date()) }
    val guardianName = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
    val guardianInitial = guardianName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "G"

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val setBoarders: (List<com.example.mbhm.model.Boarder>) -> Unit = { list ->
        scope.launch { appRepository.upsertBoarders(list) }
    }
    val setPayments: (List<com.example.mbhm.model.PaymentRecord>) -> Unit = { list ->
        scope.launch { appRepository.upsertPayments(list) }
    }
    val setRecords: (List<com.example.mbhm.model.AttendanceRecord>) -> Unit = { list ->
        scope.launch { appRepository.upsertAttendanceRecords(list) }
    }
    val setAnnouncements: (List<com.example.mbhm.model.Announcement>) -> Unit = { list ->
        scope.launch { appRepository.upsertAnnouncements(list) }
    }
    val setIncidents: (List<com.example.mbhm.model.Incident>) -> Unit = { list ->
        scope.launch { appRepository.upsertIncidents(list) }
    }
    val setMaintenance: (List<com.example.mbhm.model.MaintenanceReport>) -> Unit = { list ->
        scope.launch { appRepository.upsertMaintenanceReports(list) }
    }

    LaunchedEffect(schedule) {
        schedule?.let { appRepository.ensureTodaySessions(today, it) }
    }

    val navItems = listOf(
        NavItem("dashboard", "Home", "🏠"),
        NavItem("payments", "Payments", "💳"),
        NavItem("attendance", "Attendance", "📅"),
        NavItem("security", "Security", "🛡️"),
        NavItem("more", "More", "••"),
    )

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "dashboard" -> GDashboard(
                        boarders = boarders,
                        payments = payments,
                        attnRecords = attnRecords,
                        sessions = sessions,
                        leaveNotices = leaveNotices,
                        guardianName = guardianName,
                        guardianInitial = guardianInitial,
                        greeting = greeting,
                        onTabChange = { tab = it },
                        onAnnouncements = { tab = "announcements" },
                        onEmergencyContacts = { securityDeepLink = "contacts"; tab = "security" },
                        onProfileClick = { showProfileModal = true },
                        onBoarderInfo = { securityDeepLink = "boarders"; tab = "security" },
                        onLeaveNotices = { showLeaveNoticesModal = true }
                    )
                    "payments" -> billing?.let { b ->
                        GPayments(
                            boarders = boarders,
                            payments = payments,
                            billing = b,
                            setBoarders = setBoarders,
                            setPayments = setPayments,
                            setBilling = { nb -> scope.launch { appRepository.insertBillingSettings(nb) } },
                            showToast = { toast = it },
                            deepLinkBoarder = paymentsBoarder,
                            onDeepLinkConsumed = { paymentsBoarder = null }
                        )
                    } ?: Placeholder("💳", "Loading payments…")
                    "attendance" -> schedule?.let { s ->
                        GAttendance(
                            boarders = boarders,
                            sessions = sessions,
records = attnRecords,
        setRecords = setRecords,
        absenceAlerts = absenceAlerts,
        onSendAlerts = { alerts -> scope.launch { appRepository.upsertAbsenceAlerts(alerts) } },
                            schedule = s,
                            setSchedule = { ns -> scope.launch { appRepository.insertWorshipSchedule(ns) } },
                            showToast = { toast = it },
                            deepLinkBoarder = attendanceBoarder,
                            onDeepLinkConsumed = { attendanceBoarder = null }
                        )
                    } ?: Placeholder("📅", "Loading attendance…")
                    "announcements" -> GAnnouncements(
                        boarders = boarders,
                        announcements = announcements,
                        setAnnouncements = setAnnouncements,
                        showToast = { toast = it },
                        onBack = { tab = "dashboard" }
                    )
                    "security" -> GSecurity(
                        boarders = boarders,
                        setBoarders = setBoarders,
                        incidents = incidents,
                        setIncidents = setIncidents,
                        maintenance = maintenance,
                        setMaintenance = setMaintenance,
                        rooms = rooms,
                        jobs = jobs,
                        guardianProfile = guardianProfile,
                        onCreateBoarder = { input ->
                            appRepository.createBoarderAccount(input, guardianProfile)
                        },
                        onSetPassword = { idNumber, pwd ->
                            appRepository.setAccountPassword(idNumber, pwd)
                        },
                        deepLink = securityDeepLink,
                        onDeepLinkConsumed = { securityDeepLink = null },
                        onViewPayments = { b -> paymentsBoarder = b; tab = "payments" },
                        onViewAttendance = { b -> attendanceBoarder = b; tab = "attendance" },
                        showToast = { toast = it }
                    )
                    "more" -> {
                        val b = billing
                        val s = schedule
                        if (b != null && s != null) {
                            GMore(
                                onLogout = onLogout,
                                showToast = { toast = it },
                                boarders = boarders,
                                setBoarders = setBoarders,
                                billing = b,
                                setBilling = { nb -> scope.launch { appRepository.insertBillingSettings(nb) } },
                                schedule = s,
                                setSchedule = { ns -> scope.launch { appRepository.insertWorshipSchedule(ns) } },
                                rooms = rooms,
                                jobs = jobs,
                                onSaveRoom = { r -> scope.launch { appRepository.saveRoom(r) } },
                                onDeleteRoom = { n -> scope.launch { appRepository.deleteRoom(n) } },
                                onSaveJob = { j -> scope.launch { appRepository.saveJob(j) } },
                                onDeleteJob = { n -> scope.launch { appRepository.deleteJob(n) } }
                            )
                        } else {
                            Placeholder("⚙️", "Loading settings…")
                        }
                    }
                }
            }
            NavBar(navItems, tab, onSelect = { tab = it })
        }

        toast?.let { msg ->
            Toast(msg, onDone = { toast = null }, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 96.dp))
        }

        // Guardian Profile Modal (Req 2.A)
        Modal(
            open = showProfileModal,
            onClose = { showProfileModal = false },
            title = "Guardian Account"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(C.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(guardianInitial, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Text(guardianName, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(C.primaryLt)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Guardian", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("@${user.username} · Casa Marigold", color = C.muted, fontSize = 13.sp)

                HorizontalDivider(color = C.border)

                val monthLabel = remember {
                    java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US).format(java.util.Date())
                }
                val todaySessionIds = sessions.filter { it.date == today }.map { it.id }.toSet()
                val presentToday = attnRecords.count {
                    it.sessionId in todaySessionIds && it.status == AttendanceStatus.PRESENT
                }
                val monthPaid = payments.filter { it.period == monthLabel }.sumOf { p ->
                    when (p.status) {
                        PayStatus.PAID -> p.amountPaid ?: p.amount
                        PayStatus.PARTIAL -> p.amountPaid ?: 0
                        else -> 0
                    }
                }
                val monthDue = boarders.sumOf { it.monthlyRate }
                val pendingReceipts = payments.count { it.receiptStatus == ReceiptStatus.PENDING_REVIEW }

                PCard(padding = PaddingValues(14.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "ACCOUNT & OVERVIEW",
                            color = C.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        profileInfoRow("Full name", guardianName)
                        profileInfoRow("Username", "@${user.username}")
                        profileInfoRow("House", "Casa Marigold")
                        HorizontalDivider(color = C.border, modifier = Modifier.padding(vertical = 8.dp))
                        profileInfoRow("Boarders", "${boarders.size} registered")
                        profileInfoRow("Collected this month", "${money(monthPaid)} of ${money(monthDue)}")
                        profileInfoRow("Receipts to review", pendingReceipts.toString())
                        profileInfoRow("Present today", "$presentToday of ${boarders.size}")
                    }
                }

                HorizontalDivider(color = C.border)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.dangerLt)
                        .clickable {
                            showLogoutConfirm = true
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sign Out", color = C.danger, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Modal(
            open = showLeaveNoticesModal,
            onClose = { showLeaveNoticesModal = false },
            title = "Leave Notices"
        ) {
            var statusFilter by remember { mutableStateOf("all") }
            var fromIso by remember { mutableStateOf("") }
            var toIso by remember { mutableStateOf("") }
            val noticeContext = LocalContext.current

            if (leaveNotices.isEmpty()) {
                EmptyState(icon = "📋", title = "No leave notices")
            } else {
                SectionHeader(title = "Filter by date")
                DateRangeFilter(
                    from = fromIso,
                    to = toIso,
                    onChange = { f, t -> fromIso = f; toIso = t }
                )
                ChipBar(
                    options = listOf(
                        "All" to "all",
                        "Pending" to "pending",
                        "Approved" to "approved",
                        "Departed" to "departed",
                        "Rejected" to "rejected",
                        "Arrived" to "completed"
                    ),
                    value = statusFilter,
                    onChange = { statusFilter = it }
                )

                val filteredNotices = leaveNotices.filter { notice ->
                    (statusFilter == "all" || notice.status.name.lowercase() == statusFilter) &&
                        withinDateRange(notice.submittedAt, fromIso, toIso)
                }

                if (filteredNotices.isEmpty()) {
                    EmptyState(icon = "🔍", title = "No matching leave notices")
                }

                filteredNotices.forEach { notice ->
                    val sender = boarders.find { it.id == notice.boarderId }
                    val (label, bg0, fg) = when (notice.status) {
                        LeaveNoticeStatus.PENDING -> Triple("Pending", C.warnLt, C.warn)
                        LeaveNoticeStatus.APPROVED -> Triple("Approved", C.sageLt, C.greenText)
                        LeaveNoticeStatus.REJECTED -> Triple("Rejected", C.dangerLt, C.danger)
                        LeaveNoticeStatus.DEPARTED -> Triple("Departed", C.primaryLt, C.primary)
                        LeaveNoticeStatus.COMPLETED -> Triple("Arrived", C.blueLt, C.blueText)
                    }
                    PCard {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(notice.reason, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${notice.leaveDate} · ${notice.destination}", color = C.muted, fontSize = 12.sp)
                                }
                                Box(Modifier.clip(RoundedCornerShape(50)).background(bg0).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                "Sent by ${sender?.name ?: "Unknown boarder"} · Room ${sender?.room ?: "—"}",
                                color = C.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Submitted ${notice.submittedAt}",
                                color = C.muted,
                                fontSize = 12.sp
                            )
                            val senderPhone = sender?.phone
                            if (!senderPhone.isNullOrBlank()) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "📞 $senderPhone",
                                        color = C.muted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50))
                                            .background(C.sageLt)
                                            .clickable { dialPhone(noticeContext, senderPhone) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Call", color = C.greenText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            notice.verifiedBy?.let { verifier ->
                                val verifiedLabel = if (notice.status == LeaveNoticeStatus.REJECTED) "✕ Declined by" else "✓ Verified by"
                                Box(
                                    Modifier.clip(RoundedCornerShape(50))
                                        .background(if (notice.status == LeaveNoticeStatus.REJECTED) C.dangerLt else C.sageLt)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "$verifiedLabel $verifier" + (notice.verifiedAt?.let { " · $it" } ?: ""),
                                        color = if (notice.status == LeaveNoticeStatus.REJECTED) C.danger else C.greenText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (notice.status == LeaveNoticeStatus.PENDING) {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                appRepository.leaveNotices.update(
                                                    notice.copy(
                                                        status = LeaveNoticeStatus.APPROVED,
                                                        verifiedBy = guardianName,
                                                        verifiedAt = java.text.SimpleDateFormat(
                                                            "MMM d, yyyy · h:mm a",
                                                            java.util.Locale.ENGLISH
                                                        ).format(java.util.Date())
                                                    )
                                                )
                                            }
                                            toast = "Leave notice approved"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = C.sageLt),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✓ Approve", color = C.greenText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                appRepository.leaveNotices.update(
                                                    notice.copy(
                                                        status = LeaveNoticeStatus.REJECTED,
                                                        verifiedBy = guardianName,
                                                        verifiedAt = java.text.SimpleDateFormat(
                                                            "MMM d, yyyy · h:mm a",
                                                            java.util.Locale.ENGLISH
                                                        ).format(java.util.Date())
                                                    )
                                                )
                                            }
                                            toast = "Leave notice declined"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = C.dangerLt),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✕ Decline", color = C.danger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Column(
                                    Modifier.fillMaxWidth().padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "Planned — depart ${notice.leaveDate} ${notice.expectedDepartureTime} · return ${notice.expectedReturnDate} ${notice.expectedReturnTime}",
                                        color = C.muted,
                                        fontSize = 12.sp
                                    )
                                    notice.departedAt?.let { departed ->
                                        Text(
                                            "🚪 Actually departed $departed",
                                            color = C.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    notice.arrivedAt?.let { arrived ->
                                        Text(
                                            "🏠 Actually arrived $arrived",
                                            color = C.greenText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    when (notice.status) {
                                        LeaveNoticeStatus.APPROVED -> {
                                            Box(
                                                Modifier.fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(C.sageLt)
                                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    "✓ Approved — waiting for the boarder to tap “I'm Leaving Now”",
                                                    color = C.greenText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        LeaveNoticeStatus.DEPARTED -> {
                                            Button(
                                                onClick = {
                                                    val now = java.text.SimpleDateFormat(
                                                        "MMM d, yyyy · h:mm a",
                                                        java.util.Locale.ENGLISH
                                                    ).format(java.util.Date())
                                                    scope.launch {
                                                        appRepository.leaveNotices.update(
                                                            notice.copy(
                                                                status = LeaveNoticeStatus.COMPLETED,
                                                                arrivedAt = now
                                                            )
                                                        )
                                                    }
                                                    toast = "Arrival confirmed at $now"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = C.sage),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "🏠 Mark as Arrived",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        LeaveNoticeStatus.COMPLETED -> {
                                            Box(
                                                Modifier.fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(C.sageLt)
                                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    "✓ Trip complete — boarder is back",
                                                    color = C.greenText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder(icon: String, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, fontSize = 32.sp)
            Text(message, color = C.muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun GDashboard(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    attnRecords: List<AttendanceRecord>,
    sessions: List<AttendanceSession>,
    leaveNotices: List<LeaveNotice>,
    guardianName: String,
    guardianInitial: String,
    greeting: String,
    onTabChange: (String) -> Unit,
    onAnnouncements: () -> Unit,
    onEmergencyContacts: () -> Unit,
    onProfileClick: () -> Unit,
    onBoarderInfo: () -> Unit,
    onLeaveNotices: () -> Unit
) {
    val monthLabel = remember { java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US).format(java.util.Date()) }
    val today = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date()) }
    val todaySessionIds = sessions.filter { it.date == today }.map { it.id }.toSet()
    val sepPay = payments.filter { it.period == monthLabel }
    val paid = sepPay.count { it.status == PayStatus.PAID }
    val partial = sepPay.count { it.status == PayStatus.PARTIAL }
    val pending = sepPay.count { it.status == PayStatus.PENDING }
    val overdue = sepPay.count { it.status == PayStatus.OVERDUE }
    val present = attnRecords.count { it.sessionId in todaySessionIds && it.status == AttendanceStatus.PRESENT }
    val fullCollected = sepPay.filter { it.status == PayStatus.PAID }.sumOf { it.amount }
    val partialCollected = sepPay.filter { it.status == PayStatus.PARTIAL }.sumOf { it.amountPaid ?: 0 }
    val collected = fullCollected + partialCollected
    val totalExpected = boarders.sumOf { it.monthlyRate }

    ScrollBody(spacing = 20, padding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)) {
        // Top Header with Top-Right Guardian Profile Avatar (Req 2.A)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$greeting, 👋", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$guardianName · Casa Marigold", color = C.muted, fontSize = 14.sp)
                    Box(
                        Modifier.padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(C.primaryLt)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Guardian", color = C.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Circular Profile Picture (Req 2.A)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(C.primary)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(guardianInitial, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Fully Paid",
                value = paid,
                sub = "of ${boarders.size} boarders",
                bg = C.sageLt,
                textColor = C.greenText,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Partially Paid",
                value = partial,
                sub = "balance still due",
                bg = C.blueLt,
                textColor = C.blueText,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Present Today",
                value = present,
                sub = "${boarders.size - present} absent",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Action Needed",
                value = pending + overdue,
                sub = "$overdue overdue · $pending pending",
                bg = C.warnLt,
                textColor = C.warn,
                modifier = Modifier.weight(1f)
            )
        }

        PCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$monthLabel Collection", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(monthLabel, color = C.muted, fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(money(collected), color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("of ${money(totalExpected)}", color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(C.sage))
                    Text("Paid ${money(fullCollected)}", color = C.muted, fontSize = 11.sp)
                }
                if (partialCollected > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(C.blueBar))
                        Text("Partial ${money(partialCollected)}", color = C.muted, fontSize = 11.sp)
                    }
                }
            }
            dashCollectionBar(fullCollected, partialCollected, totalExpected)
            Text(
                "${paid} fully paid · ${partial} partial · ${pending + overdue} outstanding",
                color = C.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Grouped Quick Actions
        GuardianQuickActions(
            onAnnouncementsClick = onAnnouncements,
            onPaymentsClick = { onTabChange("payments") },
            onAttendanceClick = { onTabChange("attendance") },
            onEmergencyContactsClick = onEmergencyContacts,
            onSecurityClick = { onTabChange("security") },
            onBoarderInfoClick = onBoarderInfo,
            onLeaveNoticesClick = onLeaveNotices
        )
    }
}

// Grouped Quick Actions Component
@Composable
fun GuardianQuickActions(
    onAnnouncementsClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onEmergencyContactsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onBoarderInfoClick: () -> Unit,
    onLeaveNoticesClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            SectionHeader(title = "Notices")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dashAction(Modifier.weight(1f), "📣", "Announcements", C.card, C.border, C.text, onAnnouncementsClick)
                dashAction(Modifier.weight(1f), "📝", "Leave Notices", C.card, C.border, C.text, onLeaveNoticesClick)
            }
        }
        Column {
            SectionHeader(title = "Records")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dashAction(Modifier.weight(1f), "💳", "Payments", C.card, C.border, C.text, onPaymentsClick)
                dashAction(Modifier.weight(1f), "📅", "Attendance", C.card, C.border, C.text, onAttendanceClick)
            }
        }
        Column {
            SectionHeader(title = "People & Safety")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dashAction(Modifier.weight(1f), "👥", "Boarders", C.card, C.border, C.text, onBoarderInfoClick)
                dashAction(Modifier.weight(1f), "🛡️", "Security", C.card, C.border, C.text, onSecurityClick)
            }
        }
        Column {
            SectionHeader(title = "Emergency")
            dashAction(
                Modifier.fillMaxWidth(),
                "📞",
                "Emergency Contacts",
                C.dangerLt,
                C.danger.copy(alpha = 0.25f),
                C.danger,
                onEmergencyContactsClick
            )
        }
    }
}

@Composable
private fun dashCollectionBar(full: Int, partial: Int, total: Int) {
    val fullFrac = if (total > 0) (full.toFloat() / total).coerceIn(0f, 1f) else 0f
    val partialFrac = if (total > 0) (partial.toFloat() / total).coerceIn(0f, 1f) else 0f
    val restFrac = (1f - fullFrac - partialFrac).coerceAtLeast(0f)
    Box(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(C.bg)
    ) {
        Row(Modifier.fillMaxSize()) {
            if (fullFrac > 0f) {
                Box(Modifier.weight(fullFrac).fillMaxHeight().background(C.sage))
            }
            if (partialFrac > 0f) {
                Box(Modifier.weight(partialFrac).fillMaxHeight().background(C.blueBar))
            }
            if (restFrac > 0f) {
                Box(Modifier.weight(restFrac).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun dashAction(
    modifier: Modifier,
    icon: String,
    label: String,
    bg: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (bg == C.dangerLt) C.danger.copy(alpha = 0.15f)
                    else C.primaryLt
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 17.sp)
        }
        Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun profileInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = C.muted, fontSize = 13.sp)
        Text(
            value,
            color = C.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
