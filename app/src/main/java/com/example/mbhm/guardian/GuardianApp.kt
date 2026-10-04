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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun GuardianApp(onLogout: () -> Unit) {
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

    val boarders = remember { mutableStateOf<List<com.example.mbhm.model.Boarder>>(emptyList()) }
    val payments = remember { mutableStateOf<List<com.example.mbhm.model.PaymentRecord>>(emptyList()) }
    val attnRecords = remember { mutableStateOf<List<com.example.mbhm.model.AttendanceRecord>>(emptyList()) }
    val announcements = remember { mutableStateOf<List<com.example.mbhm.model.Announcement>>(emptyList()) }
    val incidents = remember { mutableStateOf<List<com.example.mbhm.model.Incident>>(emptyList()) }
    val maintenance = remember { mutableStateOf<List<com.example.mbhm.model.MaintenanceReport>>(emptyList()) }
    val curfewRecords = remember { mutableStateOf<List<com.example.mbhm.model.CurfewRecord>>(emptyList()) }
    val billing = remember { mutableStateOf<com.example.mbhm.model.BillingSettings?>(null) }
    val schedule = remember { mutableStateOf<com.example.mbhm.model.WorshipSchedule?>(null) }
    val sessions = remember { mutableStateOf<List<com.example.mbhm.model.AttendanceSession>>(emptyList()) }
    var leaveNotices by remember { mutableStateOf<List<com.example.mbhm.model.LeaveNotice>>(emptyList()) }
    var toast by remember { mutableStateOf<String?>(null) }
    var securityDeepLink by remember { mutableStateOf<String?>(null) }
    var showProfileModal by remember { mutableStateOf(false) }
    var showLeaveNoticesModal by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    val appRepository = remember { AppRepository.getInstance(context) }

    LaunchedEffect(Unit) {
        boarders.value = appRepository.getAllBoarders().first()
        payments.value = appRepository.getAllPayments().first()
        attnRecords.value = appRepository.getAllAttendanceRecords().first()
        announcements.value = appRepository.getAllAnnouncements().first()
        incidents.value = appRepository.getAllIncidents().first()
        maintenance.value = appRepository.getAllMaintenanceReports().first()
        curfewRecords.value = appRepository.getAllCurfewRecords().first()
        billing.value = appRepository.getBillingSettings()
        schedule.value = appRepository.getWorshipSchedule()
        sessions.value = appRepository.getAllAttendanceSessions().first()
        leaveNotices = appRepository.leaveNotices.getAll().first()
        loaded = true
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
                        boarders = boarders.value,
                        payments = payments.value,
                        attnRecords = attnRecords.value,
                        leaveNotices = leaveNotices,
                        onTabChange = { tab = it },
                        onEmergencyContacts = { securityDeepLink = "contacts"; tab = "security" },
                        onProfileClick = { showProfileModal = true },
                        onBoarderInfo = { securityDeepLink = "boarders"; tab = "security" },
                        onLeaveNotices = { showLeaveNoticesModal = true }
                    )
                    "payments" -> billing.value?.let { b ->
                        GPayments(
                            boarders = boarders.value,
                            payments = payments.value,
                            billing = b,
                            setBoarders = { boarders.value = it },
                            setPayments = { payments.value = it },
                            setBilling = { billing.value = it },
                            showToast = { toast = it }
                        )
                    } ?: Placeholder("💳", "Loading payments…")
                    "attendance" -> schedule.value?.let { s ->
                        GAttendance(
                            boarders = boarders.value,
                            sessions = sessions.value,
                            records = attnRecords.value,
                            setRecords = { attnRecords.value = it },
                            schedule = s,
                            setSchedule = { schedule.value = it },
                            showToast = { toast = it }
                        )
                    } ?: Placeholder("📅", "Loading attendance…")
                    "security" -> GSecurity(
                        boarders = boarders.value,
                        setBoarders = { boarders.value = it },
                        announcements = announcements.value,
                        setAnnouncements = { announcements.value = it },
                        incidents = incidents.value,
                        setIncidents = { incidents.value = it },
                        maintenance = maintenance.value,
                        setMaintenance = { maintenance.value = it },
                        curfew = curfewRecords.value,
                        setCurfew = { curfewRecords.value = it },
                        deepLink = securityDeepLink,
                        onDeepLinkConsumed = { securityDeepLink = null },
                        showToast = { toast = it }
                    )
                    "more" -> GMore(onLogout = onLogout, showToast = { toast = it })
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
                    Text("S", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Text("Sandra Santos", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("House Guardian · Casa Marigold", color = C.muted, fontSize = 13.sp)

                HorizontalDivider(color = C.border, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.dangerLt)
                        .clickable {
                            showProfileModal = false
                            onLogout()
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
            if (leaveNotices.isEmpty()) {
                EmptyState(icon = "📋", title = "No leave notices")
            } else {
                leaveNotices.forEach { notice ->
                    val (label, bg0, fg) = when (notice.status) {
                        LeaveNoticeStatus.PENDING -> Triple("Pending", C.warnLt, C.warn)
                        LeaveNoticeStatus.APPROVED -> Triple("Approved", C.sageLt, C.greenText)
                        LeaveNoticeStatus.REJECTED -> Triple("Rejected", C.dangerLt, C.danger)
                        LeaveNoticeStatus.COMPLETED -> Triple("Completed", C.blueLt, C.blueText)
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
                            if (notice.status == LeaveNoticeStatus.PENDING) {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                appRepository.leaveNotices.update(
                                                    notice.copy(status = LeaveNoticeStatus.APPROVED)
                                                )
                                            }
                                            leaveNotices = leaveNotices.map {
                                                if (it.id == notice.id) notice.copy(status = LeaveNoticeStatus.APPROVED) else it
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
                                                    notice.copy(status = LeaveNoticeStatus.REJECTED)
                                                )
                                            }
                                            leaveNotices = leaveNotices.map {
                                                if (it.id == notice.id) notice.copy(status = LeaveNoticeStatus.REJECTED) else it
                                            }
                                            toast = "Leave notice declined"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = C.dangerLt),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✕ Decline", color = C.danger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    leaveNotices: List<LeaveNotice>,
    onTabChange: (String) -> Unit,
    onEmergencyContacts: () -> Unit,
    onProfileClick: () -> Unit,
    onBoarderInfo: () -> Unit,
    onLeaveNotices: () -> Unit
) {
    val sepPay = payments.filter { it.period == "Sep 2026" }
    val paid = sepPay.count { it.status == PayStatus.PAID }
    val partial = sepPay.count { it.status == PayStatus.PARTIAL }
    val pending = sepPay.count { it.status == PayStatus.PENDING }
    val overdue = sepPay.count { it.status == PayStatus.OVERDUE }
    val present = attnRecords.count { it.sessionId == "s1" && it.status == AttendanceStatus.PRESENT }
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
                Text("Good morning, 👋", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Sandra Santos · Casa Marigold", color = C.muted, fontSize = 14.sp)
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
                Text("S", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                Text("September Collection", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Sep 2026", color = C.muted, fontSize = 12.sp)
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

        // Reusable Consistent Quick Actions (Req 2.B)
        GuardianQuickActions(
            onPaymentsClick = { onTabChange("payments") },
            onAttendanceClick = { onTabChange("attendance") },
            onEmergencyContactsClick = onEmergencyContacts,
            onSecurityClick = { onTabChange("security") },
            onBoarderInfoClick = onBoarderInfo,
            onLeaveNoticesClick = onLeaveNotices
        )
    }
}

// Reusable Consistent Quick Actions Component (Req 2.B)
@Composable
fun GuardianQuickActions(
    onPaymentsClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onEmergencyContactsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onBoarderInfoClick: () -> Unit,
    onLeaveNoticesClick: () -> Unit
) {
    Column {
        SectionHeader(title = "Quick Actions")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dashAction(Modifier.weight(1f), "💳", "Payments", C.card, C.border, C.text, onPaymentsClick)
            dashAction(Modifier.weight(1f), "📅", "Attendance", C.card, C.border, C.text, onAttendanceClick)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dashAction(Modifier.weight(1f), "🛡️", "Security", C.card, C.border, C.text, onSecurityClick)
            dashAction(Modifier.weight(1f), "📞", "Emergency", C.dangerLt, Color(0xFFF5C4C4), C.danger, onEmergencyContactsClick)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dashAction(Modifier.weight(1f), "👥", "Boarders", C.card, C.border, C.text, onBoarderInfoClick)
            dashAction(Modifier.weight(1f), "📝", "Leave Notices", C.card, C.border, C.text, onLeaveNoticesClick)
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
        Text(icon, fontSize = 20.sp)
        Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
